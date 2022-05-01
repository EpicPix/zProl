package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.compiled.bytecode.IBytecodeStorage;
import ga.epicpix.zprol.exceptions.CompileException;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.exceptions.UnknownTypeException;
import ga.epicpix.zprol.operation.OperationGenerator;
import ga.epicpix.zprol.operation.OperationNumber;
import ga.epicpix.zprol.operation.OperationOperator;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.TokenType;
import ga.epicpix.zprol.precompiled.PreCompiledData;
import ga.epicpix.zprol.precompiled.PreFunction;
import ga.epicpix.zprol.precompiled.PreParameter;

import java.util.ArrayList;
import java.util.Arrays;

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
                        generateInstructionsFromEquation(named.getTokenWithName("Expression").tokens, sig.returnType(), data, localsManager, storage);
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

    public static void generateInstructionsFromEquation(Token[] equation, PrimitiveType expectedType, CompiledData data, LocalScopeManager localsManager, IBytecodeStorage bytecode) {
        var operations = OperationGenerator.getOperations(new SeekIterator<>(equation));
        for(var operation : operations) {
            if(operation instanceof OperationNumber number) {
                bytecode.pushInstruction(getConstructedSizeInstruction(expectedType.getSize(), "push", number.number));
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
            }
        }
    }

    public static void compileFunction(CompiledData data, PreFunction function) throws UnknownTypeException {
        PrimitiveType returnType = data.resolveType(function.returnType);
        PrimitiveType[] parameters = new PrimitiveType[function.parameters.size()];
        String[] names = new String[function.parameters.size()];
        for(int i = 0; i<function.parameters.size(); i++) {
            PreParameter param = function.parameters.get(i);
            parameters[i] = data.resolveType(param.type);
            names[i] = param.name;
        }
        FunctionSignature signature = new FunctionSignature(returnType, parameters);
        data.addFunction(new Function(data.namespace, function.name, signature, parseFunctionCode(data, new SeekIterator<>(function.code), signature, names)));
    }

    public static CompiledData compile(PreCompiledData preCompiled, ArrayList<PreCompiledData> other) throws UnknownTypeException {
        CompiledData data = new CompiledData(preCompiled.namespace);

        for(PreCompiledData o : other) if(preCompiled.using.contains(o.namespace)) data.using(o);

        for(PreFunction function : preCompiled.functions) compileFunction(data, function);
        return data;
    }

}
