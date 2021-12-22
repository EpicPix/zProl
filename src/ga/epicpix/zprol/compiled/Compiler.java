package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.Language;
import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.compiled.bytecode.IBytecodeStorage;
import ga.epicpix.zprol.precompiled.PreCompiledData;
import ga.epicpix.zprol.operation.Operation.OperationAdd;
import ga.epicpix.zprol.operation.Operation.OperationAnd;
import ga.epicpix.zprol.operation.Operation.OperationAssignment;
import ga.epicpix.zprol.operation.Operation.OperationBrackets;
import ga.epicpix.zprol.operation.Operation.OperationCall;
import ga.epicpix.zprol.operation.Operation.OperationCast;
import ga.epicpix.zprol.operation.Operation.OperationComparison;
import ga.epicpix.zprol.operation.Operation.OperationComparisonNot;
import ga.epicpix.zprol.operation.Operation.OperationDivide;
import ga.epicpix.zprol.operation.Operation.OperationField;
import ga.epicpix.zprol.operation.Operation.OperationMod;
import ga.epicpix.zprol.operation.Operation.OperationMultiply;
import ga.epicpix.zprol.operation.Operation.OperationNumber;
import ga.epicpix.zprol.operation.Operation.OperationShiftLeft;
import ga.epicpix.zprol.operation.Operation.OperationShiftRight;
import ga.epicpix.zprol.operation.Operation.OperationString;
import ga.epicpix.zprol.operation.Operation.OperationSubtract;
import ga.epicpix.zprol.operation.OperationCompiler;
import ga.epicpix.zprol.operation.Operation;
import ga.epicpix.zprol.precompiled.PreFunction;
import ga.epicpix.zprol.precompiled.PreParameter;
import ga.epicpix.zprol.precompiled.PreStructure;
import ga.epicpix.zprol.precompiled.PreStructureField;
import ga.epicpix.zprol.exceptions.FunctionNotDefinedException;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.exceptions.UnknownTypeException;
import ga.epicpix.zprol.exceptions.VariableNotDefinedException;
import ga.epicpix.zprol.parser.tokens.OperatorToken;
import ga.epicpix.zprol.parser.tokens.ParsedToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.TokenType;
import ga.epicpix.zprol.parser.tokens.WordToken;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

import static ga.epicpix.zprol.StaticImports.createStorage;
import static ga.epicpix.zprol.StaticImports.getConstructedInstruction;
import static ga.epicpix.zprol.StaticImports.getInstruction;
import static ga.epicpix.zprol.StaticImports.getInstructionPrefix;

public class Compiler {

    public static IBytecodeStorage parseFunctionCode(ArrayList<Scope> scopes, CompiledData data, SeekIterator<Token> tokens, FunctionSignature sig, String[] names) {
        OperationCompiler mathCompiler = new OperationCompiler();
        IBytecodeStorage storage = createStorage();
        LocalScopeManager localsManager = new LocalScopeManager();
        for(int i = 0; i<names.length; i++) {
            localsManager.getCurrentScope().defineLocalVariable(names[i], sig.parameters[i]);
        }
        localsManager.newScope();
        int opens = 0;
        Token token;
        while(true) {
            token = tokens.next();
            if(token.getType() == TokenType.PARSED) {
                ParsedToken parsed = (ParsedToken) token;
                if(parsed.name.equals("Return")) {
                    if(sig.returnType.getSize() == 0) storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(0) + "return"));
                    else throw new RuntimeException("Tried to return no value while the method is not void");
                    break;
                }else if(parsed.name.equals("ReturnValue")) {
                    if(sig.returnType.getSize() != 0) {
                        convertOperationToBytecode(scopes, storage, localsManager, data, mathCompiler.compile(data, new SeekIterator<>(parsed.tokens.get(1).asEquationToken().tokens)), true, sig.returnType);
                        storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(sig.returnType.getSize()) + "return"));
                    } else throw new RuntimeException("Tried to return a value while the method is void");
                    break;
                }else if(parsed.name.equals("Call")) {
                    convertOperationToBytecode(scopes, storage, localsManager, data, mathCompiler.compile(data, new SeekIterator<>(parsed.tokens)), false, null);
                }else {
                    throw new RuntimeException("Not implemented language feature: " + parsed.name + " / " + parsed.tokens);
                }
            }else if(token.getType() == TokenType.WORD) {
                WordToken w = (WordToken) token;
                if(w.getWord().equals("if")) {
                    throw new NotImplementedException("Keyword 'if' is not implemented");
                }else if(w.getWord().equals("while")) {
                    throw new NotImplementedException("Keyword 'while' is not implemented");
                }else {
                    int startIndex = tokens.currentIndex() - 1;
                    try {
                        tokens.back();
                        Type type = data.resolveType(tokens.next().asWordHolder().getWord());
                        token = tokens.next();
                        if(token.getType() != TokenType.WORD) {
                            throw new RuntimeException("Cannot handle this token: " + token);
                        }
                        String name = token.asWordToken().getWord();
                        LocalVariable lVar = localsManager.defineLocalVariable(name, type);
                        token = tokens.next();
                        if(token.getType() != TokenType.END_LINE) {
                            if(token.getType() == TokenType.OPERATOR) {
                                if(((OperatorToken) token).operator.equals("=")) {
                                    convertOperationToBytecode(scopes, storage, localsManager, data, mathCompiler.compile(data, tokens), true, type);
                                    storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(type.getSize()) + "store_local", lVar.index));
                                }else {
                                    throw new RuntimeException("Cannot handle this token: " + token);
                                }
                            }else {
                                throw new RuntimeException("Cannot handle this token: " + token);
                            }
                        }
                    } catch (UnknownTypeException unkType) {
                        tokens.setIndex(startIndex);
                        convertOperationToBytecode(scopes, storage, localsManager, data, mathCompiler.compile(data, tokens), false, null);
                    }
                }
            }else {
                if(token.getType() == TokenType.OPEN_SCOPE) {
                    opens++;
                    localsManager.newScope();
                    continue;
                }else if(token.getType() == TokenType.CLOSE_SCOPE) {
                    opens--;
                    localsManager.leaveScope();
                    if(opens == 0) {
                        localsManager.leaveScope();
                        break;
                    }
                    continue;
                }
                //TODO: Not sure what this could be, maybe ++i or --i
                throw new RuntimeException("Cannot handle this token: " + token);
            }
        }
        storage.setLocalsSize(localsManager.getLocalVariablesSize());
        return storage;
    }

    public static void convertOperationToBytecode(ArrayList<Scope> scopes, IBytecodeStorage storage, LocalScopeManager localsManager, CompiledData data, Operation op, boolean returnRequired, Type t) {
        int size = 0;
        boolean unsigned = false;
        BigInteger biggestNumber = BigInteger.ZERO;
        BigInteger smallestNumber = BigInteger.ZERO;

        if(t != null && t.isNumberType()) {
            size = t.getSize();
            unsigned = t.isUnsigned();
            if(unsigned) {
                biggestNumber = BigDecimal.valueOf(Math.pow(2, size * 8) - 1).toBigInteger();
            } else {
                biggestNumber = BigDecimal.valueOf(Math.pow(2, size * 8 - 1) - 1).toBigInteger();
                smallestNumber = BigDecimal.valueOf(-Math.pow(2, size * 8 - 1)).toBigInteger();
            }
        }

        if(op instanceof OperationBrackets) {
            convertOperationToBytecode(scopes, storage, localsManager, data, op.left, true, t);
        }else if(op instanceof OperationCall call) {
            if(call.reference.get(0).asWordToken().getWord().equals("syscall")) {
                int params = call.parameters.size();
                if(params > 7 || params <= 0) {
                    throw new FunctionNotDefinedException("syscall" + params);
                }

                for(int i = 0; i<call.parameters.size(); i++) {
                    convertOperationToBytecode(scopes, storage, localsManager, data, call.parameters.get(i), true, Language.TYPES.get("uint64"));
                }

                if(params == 1) storage.pushInstruction(getConstructedInstruction("syscall1"));
                else if(params == 2) storage.pushInstruction(getConstructedInstruction("syscall2"));
                else if(params == 3) storage.pushInstruction(getConstructedInstruction("syscall3"));
                else if(params == 4) storage.pushInstruction(getConstructedInstruction("syscall4"));
                else if(params == 5) storage.pushInstruction(getConstructedInstruction("syscall5"));
                else if(params == 6) storage.pushInstruction(getConstructedInstruction("syscall6"));
                else storage.pushInstruction(getConstructedInstruction("syscall7"));

                if(!returnRequired) {
                    storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(8) + "pop"));
                }

                return;
            }
            ArrayList<Type> parameters = new ArrayList<>();
            for(int i = 0; i<call.parameters.size(); i++) {
                Operation param = call.parameters.get(i);
                if(param instanceof OperationField f) {
                    if(f.reference.size() != 1) {
                        throw new NotImplementedException("Not implemented yet");
                    }
                    LocalVariable var = localsManager.getLocalVariable(f.reference.get(0).asWordToken().getWord());
                    parameters.add(var.type);
                }else {
                    throw new NotImplementedException("Not implemented yet");
                }
            }
            if(call.reference.size() != 1) {
                throw new NotImplementedException("Not implemented yet");
            }
            LocalVariable v = localsManager.findLocalVariable(call.reference.get(0).asWordToken().getWord());
            FunctionSignature ss = new FunctionSignature(null, parameters.toArray(new Type[0]));
            if(v == null) {
                Function func = data.getFunction(call.reference.get(0).asWordToken().getWord(), ss);
                for(int i = func.signature.parameters.length - 1; i >= 0; i--) {
                    convertOperationToBytecode(scopes, storage, localsManager, data, call.parameters.get(i), true, func.signature.parameters[i]);
                }
                throw new NotImplementedException("Not implemented yet");

//                if(!returnRequired) {
//                    int retSize = func.signature.returnType.getSize();
//                    if(retSize != 0) storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(retSize) + "pop"));
//                }
            }else {
                throw new NotImplementedException("Not implemented yet");
            }
        }else if(op instanceof OperationField) {
            SeekIterator<Token> tokens = new SeekIterator<>(((OperationField) op).reference);
            while(tokens.hasNext()) {
                Token token = tokens.next();
                if(token.getType() == TokenType.WORD) {
                    LocalVariable var = localsManager.findLocalVariable(token.asWordToken().getWord());
                    int psize = 0;
                    if(var != null) {
                        psize = var.type.getSize();
                        storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(psize) + "load_local", var.index));
                    }else {
                        throw new NotImplementedException("Not implemented yet");
                    }

                    storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(psize) + "cast" + getInstructionPrefix(t.getSize())));

                }else if(token.getType() == TokenType.ACCESSOR) {
                    throw new NotImplementedException("Dereferencing is not implemented yet");
                }else {
                    throw new NotImplementedException("Cannot handle token: " + token);
                }
            }
        }else if(op instanceof OperationNumber) {
            BigInteger num = (((OperationNumber) op).number).number;
            if(numberInBounds(smallestNumber, biggestNumber, num)) {
                storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(size) + "push", num));
            }else {
                throw new RuntimeException("Number " + num + " is not in range of the type (" + smallestNumber + " to " + biggestNumber + ")");
            }
        }else if(op instanceof OperationString) {
            //TODO
        }else if(op instanceof OperationCast) {
            if(t == null) throw new NotImplementedException("Not supported cast");
            if(((OperationCast) op).type == null) throw new NotImplementedException("Not supported cast");
            convertOperationToBytecode(scopes, storage, localsManager, data, op.left, true, ((OperationCast) op).type);

            Type t2 = ((OperationCast) op).type;
            storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(t2.getSize()) + "cast" + getInstructionPrefix(t.getSize())));
        }else if(op instanceof OperationAssignment) {
            ArrayList<Token> tokens = ((OperationField) op.left).reference;

            try {
                LocalVariable lVar;

                if(tokens.size() == 1) {
                    lVar = localsManager.getLocalVariable(tokens.get(0).asWordToken().getWord());
                } else {
                    throw new NotImplementedException("TODO");
                }
                convertOperationToBytecode(scopes, storage, localsManager, data, op.right, true, lVar.type);
                storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(lVar.type.getSize()) + "store_local", lVar.index));
            }catch(VariableNotDefinedException e) {
                e.printStackTrace();
            }

        }else if(op instanceof OperationComparison) {
            convertOperationToBytecode(scopes, storage, localsManager, data, op.left, true, Language.TYPES.get("uint64"));
            convertOperationToBytecode(scopes, storage, localsManager, data, op.right, true, Language.TYPES.get("uint64"));
            storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(8) + "compare"));
        }else if(op instanceof OperationComparisonNot) {
            convertOperationToBytecode(scopes, storage, localsManager, data, op.left, true, Language.TYPES.get("uint64"));
            convertOperationToBytecode(scopes, storage, localsManager, data, op.right, true, Language.TYPES.get("uint64"));
            storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(8) + "compare_not"));
        }else {
            convertOperationToBytecode(scopes, storage, localsManager, data, op.left, true, t);
            convertOperationToBytecode(scopes, storage, localsManager, data, op.right, true, t);
            if(op instanceof OperationAdd) {
                storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(size) + "add"));
            }else if(op instanceof OperationSubtract) {
                storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(size) + "sub"));
            }else if(op instanceof OperationMultiply) {
                if(unsigned) storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(size) + "mulu"));
                else storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(size) + "mul"));
            }else if(op instanceof OperationDivide) {
                if(unsigned) storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(size) + "divu"));
                else storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(size) + "div"));
            }else if(op instanceof OperationMod) {
                if(unsigned) storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(size) + "modu"));
                else storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(size) + "mod"));
            }else if(op instanceof OperationAnd) {
                storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(size) + "and"));
            }else if(op instanceof OperationShiftLeft) {
                storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(size) + "shift_left"));
            }else if(op instanceof OperationShiftRight) {
                storage.pushInstruction(getConstructedInstruction(getInstructionPrefix(size) + "shift_right"));
            }else {
                throw new NotImplementedException(op.getClass().getSimpleName() + " is not implemented!");
            }
        }
    }

    private static boolean numberInBounds(BigInteger lowest, BigInteger highest, BigInteger num) {
        return num.compareTo(lowest) >= 0 && highest.compareTo(num) >= 0;
    }

    public static void compileFunction(ArrayList<Scope> scopes, CompiledData data, PreFunction function) throws UnknownTypeException {
//        ArrayList<Flag> flags = convertFlags(function.flags);
        Type returnType = data.resolveType(function.returnType);
        Type[] parameters = new Type[function.parameters.size()];
        String[] names = new String[function.parameters.size()];
        for(int i = 0; i<function.parameters.size(); i++) {
            PreParameter param = function.parameters.get(i);
            parameters[i] = data.resolveType(param.type);
            names[i] = param.name;
        }
        FunctionSignature signature = new FunctionSignature(returnType, parameters);
//        if(flags.contains(Flag.NO_IMPLEMENTATION)) {
//            data.addFunction(new Function(function.name, signature, flags, null));
//        }
        data.addFunction(new Function(function.name, signature, new ArrayList<>(), parseFunctionCode(scopes, data, new SeekIterator<>(function.code), signature, names)));
    }

    public static void compileStructure(CompiledData data, PreStructure structure) throws UnknownTypeException {
        ArrayList<StructureField> fields = new ArrayList<>(structure.fields.size());
        for(int i = 0; i<structure.fields.size(); i++) {
            PreStructureField field = structure.fields.get(i);
            fields.add(new StructureField(field.name, data.resolveType(field.type)));
        }
        data.addStructure(new Structure(structure.name, fields));
    }

    public static CompiledData compile(PreCompiledData preCompiled, ArrayList<PreCompiledData> other) throws UnknownTypeException {
        ArrayList<PreCompiledData> used = new ArrayList<>();
        for(PreCompiledData o : other) if(preCompiled.using.contains(o.namespace)) used.add(o);

        CompiledData data = new CompiledData(preCompiled.namespace);
//        data.importData(imported); //TODO: Add  CompiledData.importData(PreCompiledData...)  method
        for(PreStructure structure : preCompiled.structures.values()) compileStructure(data, structure);
        for(PreFunction function : preCompiled.functions) compileFunction(new ArrayList<>(), data, function);
        return data;
    }

}
