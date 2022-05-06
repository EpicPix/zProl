package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.bytecode.IBytecodeStorage;
import ga.epicpix.zprol.compiled.locals.LocalScopeManager;
import ga.epicpix.zprol.exceptions.compilation.CompileException;
import ga.epicpix.zprol.exceptions.compilation.FunctionNotDefinedException;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.exceptions.compilation.UnknownTypeException;
import ga.epicpix.zprol.operation.*;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.TokenType;
import ga.epicpix.zprol.precompiled.*;
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
                if("ReturnStatement".equals(named.name)) {
                    if(!sig.returnType().isBuiltInType() || sig.returnType().getSize() != 0) {
                        if(named.getTokenWithName("Expression") == null) {
                            throw new CompileException("Function is not void, expected a return value", named);
                        }
                        generateInstructionsFromEquation(OperationGenerator.getOperations(new SeekIterator<>(named.getTokenWithName("Expression").tokens)), sig.returnType(), data, localsManager, storage, false);
                    }
                    if(sig.returnType().isBuiltInType() && sig.returnType().getSize() == 0) {
                        if(named.getTokenWithName("Expression") != null) {
                            throw new CompileException("Function is void, expected no value", named);
                        }
                    }
                    storage.pushInstruction(getConstructedSizeInstruction(sig.returnType().getSize(), "return"));
                    if(opens == 0) {
                        hasReturned = true;
                        break;
                    }
                } else if("FunctionCallStatement".equals(named.name)) {
                    generateInstructionsFromEquation(OperationGenerator.getOperations(new SeekIterator<>(new Token[] {named})), null, data, localsManager, storage, true);
                } else if("CreateAssignmentStatement".equals(named.name)) {
                    var type = data.resolveType(named.getSingleTokenWithName("Type").asWordToken().getWord());
                    var name = named.getSingleTokenWithName("Identifier").asWordToken().getWord();
                    var expression = named.getTokenWithName("Expression").tokens;
                    generateInstructionsFromEquation(OperationGenerator.getOperations(new SeekIterator<>(expression)), type, data, localsManager, storage, false);
                    var local = localsManager.defineLocalVariable(name, type);
                    storage.pushInstruction(getConstructedSizeInstruction(local.type().getSize(), "store_local", local.index()));
                } else if("AssignmentStatement".equals(named.name)) {
                    var name = named.getSingleTokenWithName("Identifier").asWordToken().getWord();
                    var expression = named.getTokenWithName("Expression").tokens;
                    var local = localsManager.getLocalVariable(name);
                    generateInstructionsFromEquation(OperationGenerator.getOperations(new SeekIterator<>(expression)), local.type(), data, localsManager, storage, false);
                    storage.pushInstruction(getConstructedSizeInstruction(local.type().getSize(), "store_local", local.index()));
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
            if(!sig.returnType().isBuiltInType() || sig.returnType().getSize() != 0) throw new CompileException("Missing return statement", tokens.current());
            storage.pushInstruction(getConstructedSizeInstruction(0, "return"));
        }
        storage.setLocalsSize(localsManager.getLocalVariablesSize());
        return storage;
    }

    public static PrimitiveType generateInstructionsFromEquation(Operation operation, PrimitiveType expectedType, CompiledData data, LocalScopeManager localsManager, IBytecodeStorage bytecode, boolean discardValue) {
        if(operation instanceof OperationRoot root) {
            for(var op : root.getOperations()) {
                generateInstructionsFromEquation(op, expectedType, data, localsManager, bytecode, expectedType == null && discardValue);
            }

            if(expectedType != null && discardValue) {
                bytecode.pushInstruction(getConstructedSizeInstruction(expectedType.getSize(), "pop"));
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
                case "|":
                    bytecode.pushInstruction(getConstructedSizeInstruction(expectedType.getSize(), "or"));
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
                generateInstructionsFromEquation(call.getOperations().get(i), parameters[i], data, localsManager, bytecode, false);
            }

            bytecode.pushInstruction(getConstructedInstruction("invoke", new Function(data.namespace, modifiers, func.name, signature, null)));

            if(discardValue && returnType.getSize() != 0) {
                bytecode.pushInstruction(getConstructedSizeInstruction(returnType.getSize(), "pop"));
            }else {
                if(expectedType != null && returnType.getSize() != expectedType.getSize()) {
                    // what about unsigned and signed?
                    return doCast(returnType, expectedType, bytecode);
                }
            }

            return returnType;
        }else if(operation instanceof OperationField field) {
            var local = localsManager.tryGetLocalVariable(field.getIdentifier());
            if(local != null) {
                bytecode.pushInstruction(getConstructedSizeInstruction(local.type().getSize(), "load_local", local.index()));
                return local.type();
            }else {
                throw new NotImplementedException("Not implemented looking in different scopes");
            }
        }else if(operation instanceof OperationString str) {
            bytecode.pushInstruction(getConstructedInstruction("push_string", str.getString().replace("\\\"", "\"").replace("\\n", "\n")));
            return Language.TYPES.get("uint64");
        }else if(operation instanceof OperationAssignment assignment) {
            var local = localsManager.tryGetLocalVariable(assignment.getIdentifier());
            if(local != null) {
                generateInstructionsFromEquation(assignment.getOperation(), local.type(), data, localsManager, bytecode, false);
                bytecode.pushInstruction(getConstructedSizeInstruction(local.type().getSize(), "dup"));
                bytecode.pushInstruction(getConstructedSizeInstruction(local.type().getSize(), "store_local", local.index()));
                return local.type();
            }else {
                throw new NotImplementedException("Not implemented looking in different scopes");
            }
        }else {
            throw new NotImplementedException("Unknown operation " + operation.getClass());
        }
    }

    public static PrimitiveType doCast(PrimitiveType from, PrimitiveType to, IBytecodeStorage bytecode) {
        if(to.getSize() > from.getSize()) {
            bytecode.pushInstruction(getConstructedSizeInstruction(from.getSize(), "cast" + getInstructionPrefix(to.getSize())));
            return to;
        }
        throw new CompileException("Unsupported cast from " + from.name + " to " + to.name);
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

    public static void compileClass(CompiledData data, PreClass clazz) {
        ClassField[] fields = new ClassField[clazz.fields.size()];
        for (int i = 0; i < clazz.fields.size(); i++) {
            PreField field = clazz.fields.get(i);
            fields[i] = new ClassField(field.name, data.resolveType(field.type));
        }
        data.addClass(new Class(data.namespace, clazz.name, fields));
    }

    public static CompiledData compile(PreCompiledData preCompiled, ArrayList<PreCompiledData> other) throws UnknownTypeException {
        CompiledData data = new CompiledData(preCompiled.namespace);

        data.using(preCompiled);
        for(PreCompiledData o : other) if(preCompiled.using.contains(o.namespace)) data.using(o);

        for(PreClass clazz : preCompiled.classes) compileClass(data, clazz);
        for(PreFunction function : preCompiled.functions) compileFunction(data, function);
        return data;
    }

}
