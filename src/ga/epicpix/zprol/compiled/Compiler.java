package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.compiled.bytecode.IBytecodeStorage;
import ga.epicpix.zprol.exceptions.CompileException;
import ga.epicpix.zprol.exceptions.FunctionNotDefinedException;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.exceptions.UnknownTypeException;
import ga.epicpix.zprol.operation.*;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.TokenType;
import ga.epicpix.zprol.precompiled.PreCompiledData;
import ga.epicpix.zprol.precompiled.PreFunction;
import ga.epicpix.zprol.precompiled.PreFunctionModifiers;
import ga.epicpix.zprol.precompiled.PreParameter;
import ga.epicpix.zprol.zld.Language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;

import static ga.epicpix.zprol.StaticImports.*;

public class Compiler {

    public static IBytecodeStorage parseFunctionCode(CompiledData data, SeekIterator<Token> tokens, FunctionSignature sig, String[] names) {
        IBytecodeStorage storage = createStorage();
        LocalScopeManager localsManager = new LocalScopeManager();
        for(int i = 0; i<names.length; i++) {
            localsManager.getCurrentScope().defineLocalVariable(names[i], sig.parameters()[i]);
        }
        localsManager.newScope();
        int opens = 0;
        boolean hasReturned = false;
        Token token;
        while(tokens.hasNext()) {
            token = tokens.next();
            if(token.getType() == TokenType.NAMED) {
                var named = (NamedToken) token;
                if("Return".equals(named.name)) {
                    if(!sig.returnType().isBuiltInType() || sig.returnType().getSize() != 0) {
                        if(named.getTokenWithName("Expression") == null) {
                            throw new CompileException("Function is not void, expected a return value");
                        }
                        generateInstructionsFromEquation(OperationGenerator.getOperations(new SeekIterator<>(named.getTokenWithName("Expression").tokens)), sig.returnType(), data, localsManager, storage);
                    }
                    if(sig.returnType().isBuiltInType() && sig.returnType().getSize() == 0) {
                        if(named.getTokenWithName("Expression") != null) {
                            throw new CompileException("Function is void, expected no value");
                        }
                    }
                    storage.pushInstruction(getConstructedSizeInstruction(sig.returnType().getSize(), "return"));
                    if(opens == 0) {
                        hasReturned = true;
                        break;
                    }
                } else if("FunctionCall".equals(named.name)) {
                    generateInstructionsFromEquation(OperationGenerator.getOperations(new SeekIterator<>(new Token[] {named})), null, data, localsManager, storage);
                } else {
                    throw new NotImplementedException("Not implemented language feature: " + named.name + " / " + Arrays.toString(named.tokens));
                }
            }else {
                if(token.getType() == TokenType.OPEN_SCOPE) {
                    opens++;
                    localsManager.newScope();
                }else if(token.getType() == TokenType.CLOSE_SCOPE) {
                    opens--;
                    localsManager.leaveScope();
                    if(opens == 0) {
                        localsManager.leaveScope();
                        break;
                    }
                }else {
                    throw new RuntimeException("Not implemented token type: " + token);
                }
                //TODO: ++x or --x
            }
        }
        if(!hasReturned) {
            if(!sig.returnType().isBuiltInType() || sig.returnType().getSize() != 0) throw new CompileException("Missing return statement");
            storage.pushInstruction(getConstructedSizeInstruction(0, "return"));
        }
        storage.setLocalsSize(localsManager.getLocalVariablesSize());
        return storage;
    }

    public static PrimitiveType generateInstructionsFromEquation(Operation operation, PrimitiveType expectedType, CompiledData data, LocalScopeManager localsManager, IBytecodeStorage bytecode) {
        if(operation instanceof OperationRoot root) {
            for(var op : root.getOperations()) {
                generateInstructionsFromEquation(op, expectedType, data, localsManager, bytecode);
            }
            return expectedType;
        }else if(operation instanceof OperationNumber number) {
            bytecode.pushInstruction(getConstructedSizeInstruction(expectedType.getSize(), "push", number.number));
            return expectedType;
        }else if(operation instanceof OperationOperator operator) {
            String op = operator.operator.operator();
            switch (op) {
                case "+":
                    bytecode.pushInstruction(getConstructedSizeInstruction(expectedType.getSize(), "add"));
                    break;
                case "-":
                    bytecode.pushInstruction(getConstructedSizeInstruction(expectedType.getSize(), "sub"));
                    break;
                case "*":
                    if (expectedType.isUnsigned()) {
                        bytecode.pushInstruction(getConstructedSizeInstruction(expectedType.getSize(), "mulu"));
                    } else {
                        bytecode.pushInstruction(getConstructedSizeInstruction(expectedType.getSize(), "mul"));
                    }
                    break;
                case "/":
                    if (expectedType.isUnsigned()) {
                        bytecode.pushInstruction(getConstructedSizeInstruction(expectedType.getSize(), "divu"));
                    } else {
                        bytecode.pushInstruction(getConstructedSizeInstruction(expectedType.getSize(), "div"));
                    }
                    break;
                default:
                    throw new NotImplementedException("Unknown operator " + op);
            }
            return expectedType;
        }else if(operation instanceof OperationCall call) {
            ArrayList<PreFunction> possibleFunctions = new ArrayList<>();
            for(var using : data.getUsing()) {
                if(data.namespace != null && !data.namespace.equals(using.namespace)) continue; // currently, method name can only be a string without dots
                for(var func : using.functions) {
                    if(func.name.equals(call.getFunctionName())) {
                        if(func.parameters.size() == call.getOperations().size()) {
                            possibleFunctions.add(func);
                        }
                    }
                }
            }

            if(possibleFunctions.size() == 0) {
                throw new FunctionNotDefinedException("Unknown function: " + call.getFunctionName());
            }else if(possibleFunctions.size() != 1) {
                throw new NotImplementedException("Cannot match overloaded parameter types");
            }

            var func = possibleFunctions.get(0);
            PrimitiveType returnType = data.resolveType(func.returnType);
            PrimitiveType[] parameters = new PrimitiveType[func.parameters.size()];
            for(int i = 0; i<func.parameters.size(); i++) {
                parameters[i] = data.resolveType(func.parameters.get(i).type);
            }
            FunctionSignature signature = new FunctionSignature(returnType, parameters);
            EnumSet<FunctionModifiers> modifiers = EnumSet.noneOf(FunctionModifiers.class);
            for(PreFunctionModifiers modifier : func.modifiers) {
                modifiers.add(modifier.getCompiledModifier());
            }

            for(int i = 0; i<call.getOperations().size(); i++) {
                generateInstructionsFromEquation(call.getOperations().get(i), parameters[i], data, localsManager, bytecode);
            }

            bytecode.pushInstruction(getConstructedInstruction("invoke", new Function(data.namespace, modifiers, func.name, signature, null)));

            return null;
        }else {
            throw new NotImplementedException("Unknown operation " + operation.getClass());
        }
    }

    public static void compileFunction(CompiledData data, PreFunction function) {
        PrimitiveType returnType = data.resolveType(function.returnType);
        PrimitiveType[] parameters = new PrimitiveType[function.parameters.size()];
        String[] names = new String[function.parameters.size()];
        for(int i = 0; i<function.parameters.size(); i++) {
            PreParameter param = function.parameters.get(i);
            parameters[i] = data.resolveType(param.type);
            names[i] = param.name;
        }
        FunctionSignature signature = new FunctionSignature(returnType, parameters);
        IBytecodeStorage bytecode = null;
        if(function.hasCode()) {
            bytecode = parseFunctionCode(data, new SeekIterator<>(function.code), signature, names);
        }
        EnumSet<FunctionModifiers> modifiers = EnumSet.noneOf(FunctionModifiers.class);
        for(PreFunctionModifiers modifier : function.modifiers) {
            modifiers.add(modifier.getCompiledModifier());
        }
        data.addFunction(new Function(data.namespace, modifiers, function.name, signature, bytecode));
    }

    public static CompiledData compile(PreCompiledData preCompiled, ArrayList<PreCompiledData> other) throws UnknownTypeException {
        CompiledData data = new CompiledData(preCompiled.namespace);

        data.using(preCompiled);
        for(PreCompiledData o : other) if(preCompiled.using.contains(o.namespace)) data.using(o);

        for(PreFunction function : preCompiled.functions) compileFunction(data, function);
        return data;
    }

}
