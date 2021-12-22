package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.Language;
import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.compiled.CompileOperation;
import ga.epicpix.zprol.compiled.CompileOperationType;
import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.compiled.Flag;
import ga.epicpix.zprol.compiled.Function;
import ga.epicpix.zprol.precompiled.PreCompiledData;
import ga.epicpix.zprol.compiled.Scope;
import ga.epicpix.zprol.compiled.FunctionSignature;
import ga.epicpix.zprol.compiled.bytecode.Bytecode;
import ga.epicpix.zprol.compiled.LocalVariable;
import ga.epicpix.zprol.compiled.bytecode.BytecodeInstructions;
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
import ga.epicpix.zprol.compiled.ObjectField;
import ga.epicpix.zprol.compiled.Structure;
import ga.epicpix.zprol.compiled.StructureField;
import ga.epicpix.zprol.compiled.Type;
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
import java.util.Stack;

public class Compiler {

    public static Bytecode parseFunctionCode(ArrayList<Scope> scopes, CompiledData data, SeekIterator<Token> tokens, FunctionSignature sig, String[] names) {
        OperationCompiler mathCompiler = new OperationCompiler();
        Bytecode bytecode = new Bytecode();
        for(int i = 0; i<names.length; i++) {
            bytecode.getCurrentScope().defineLocalVariable(names[i], sig.parameters[i]);
        }
        bytecode.newScope();
        int opens = 0;
        Stack<CompileOperation> operations = new Stack<>();
        Token token;
        while(true) {
            token = tokens.next();
            if(token.getType() == TokenType.PARSED) {
                ParsedToken parsed = (ParsedToken) token;
                if(parsed.name.equals("Return")) {
                    if(sig.returnType.getSize() == 0) bytecode.pushInstruction(BytecodeInstructions.RETURN);
                    else throw new RuntimeException("Tried to return no value while the method is not void");
                    break;
                }else if(parsed.name.equals("ReturnValue")) {
                    if(sig.returnType.getSize() != 0) {
                        convertOperationToBytecode(scopes, bytecode, data, mathCompiler.compile(data, new SeekIterator<>(parsed.tokens.get(1).asEquationToken().tokens)), true, sig.returnType);
                        bytecode.pushSizedInstruction("RETURN", sig.returnType.getSize());
                    } else throw new RuntimeException("Tried to return a value while the method is void");
                    break;
                }else if(parsed.name.equals("Call")) {
                    convertOperationToBytecode(scopes, bytecode, data, mathCompiler.compile(data, new SeekIterator<>(parsed.tokens)), false, null);
                }else {
                    throw new RuntimeException("Not implemented language feature: " + parsed.name + " / " + parsed.tokens);
                }
            }else if(token.getType() == TokenType.WORD) {
                WordToken w = (WordToken) token;
                if(w.getWord().equals("if")) {
                    if(tokens.next().getType() != TokenType.OPEN) {
                        throw new RuntimeException("Missing '('");
                    }
                    ArrayList<Operation> op = new ArrayList<>();
                    mathCompiler.compile0(1, op, new Stack<>(), tokens, data);
                    convertOperationToBytecode(scopes, bytecode, data, op.get(0), tokens.seek().getType() == TokenType.OPEN, null);
                    if(tokens.seek().getType() == TokenType.OPEN) {
                        operations.push(new CompileOperation(bytecode.pushInstruction(BytecodeInstructions.JUMPNE, (short) bytecode.getInstructions().size()), CompileOperationType.IF));
                    }else if(tokens.seek().getType() != TokenType.CLOSE) {
                        throw new RuntimeException("Unknown symbol: '" + tokens.seek() + "'");
                    }
                }else if(w.getWord().equals("while")) {
                    if(tokens.next().getType() != TokenType.OPEN) {
                        throw new RuntimeException("Missing '('");
                    }
                    ArrayList<Operation> op = new ArrayList<>();
                    mathCompiler.compile0(1, op, new Stack<>(), tokens, data);
                    int s = bytecode.getInstructions().size();
                    convertOperationToBytecode(scopes, bytecode, data, op.get(0), tokens.seek().getType() == TokenType.OPEN, null);
                    if(tokens.seek().getType() == TokenType.OPEN) {
                        operations.push(new CompileOperation(bytecode.pushInstruction(BytecodeInstructions.JUMPNE, (short) s, (short) bytecode.getInstructions().size()), CompileOperationType.WHILE));
                    }else if(tokens.seek().getType() != TokenType.CLOSE) {
                        throw new RuntimeException("Unknown symbol: '" + tokens.seek() + "'");
                    }
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
                        LocalVariable lVar = bytecode.getCurrentScope().defineLocalVariable(name, type);
                        token = tokens.next();
                        if(token.getType() != TokenType.END_LINE) {
                            if(token.getType() == TokenType.OPERATOR) {
                                if(((OperatorToken) token).operator.equals("=")) {
                                    convertOperationToBytecode(scopes, bytecode, data, mathCompiler.compile(data, tokens), true, type);
                                    bytecode.pushSizedInstruction("STORE", type.getSize(), (short) lVar.index);
                                }else {
                                    throw new RuntimeException("Cannot handle this token: " + token);
                                }
                            }else {
                                throw new RuntimeException("Cannot handle this token: " + token);
                            }
                        }
                    } catch (UnknownTypeException unkType) {
                        tokens.setIndex(startIndex);
                        convertOperationToBytecode(scopes, bytecode, data, mathCompiler.compile(data, tokens), false, null);
                    }
                }
            }else {
                if(token.getType() == TokenType.OPEN_SCOPE) {
                    opens++;
                    bytecode.newScope();
                    continue;
                }else if(token.getType() == TokenType.CLOSE_SCOPE) {
                    opens--;
                    bytecode.leaveScope();
                    if(opens == 0) {
                        bytecode.leaveScope();
                        break;
                    }
                    if(operations.size() != 0) {
                        CompileOperation i = operations.pop();
                        short s = (short) i.instruction.data[0];
                        i.instruction.data[0] = (short) (bytecode.getInstructions().size() - s);
                        if(i.type == CompileOperationType.IF) {
                            if(tokens.seek().getType() == TokenType.WORD && tokens.seek().asWordToken().getWord().equals("else")) {
                                tokens.next();
                                Token w = tokens.seek();
                                if(w instanceof WordToken && w.asWordToken().getWord().equals("if")) {
                                    tokens.next();
                                    if(tokens.next().getType() != TokenType.OPEN) {
                                        throw new RuntimeException("Missing '('");
                                    }
                                    ArrayList<Operation> op = new ArrayList<>();
                                    mathCompiler.compile0(1, op, new Stack<>(), tokens, data);
                                    convertOperationToBytecode(scopes, bytecode, data, op.get(0), tokens.seek().getType() == TokenType.OPEN, null);
                                    if(tokens.seek().getType() == TokenType.OPEN) {
                                        operations.push(new CompileOperation(bytecode.pushInstruction(BytecodeInstructions.JUMPNE, (short) bytecode.getInstructions().size()), CompileOperationType.IF));
                                    }else if(tokens.seek().getType() != TokenType.CLOSE) {
                                        throw new RuntimeException("Unknown symbol: '" + tokens.seek() + "'");
                                    }
                                }else {
                                    i.instruction.data[0] = (short) ((short) i.instruction.data[0] + 1);
                                    operations.push(new CompileOperation(bytecode.pushInstruction(BytecodeInstructions.JUMP, (short) bytecode.getInstructions().size()), CompileOperationType.ELSE));
                                }
                            }
                        }else if(i.type == CompileOperationType.WHILE) {
                            short pos = (short) i.instruction.data[1];
                            i.instruction.data = new java.lang.Object[] {i.instruction.data[0]};
                            bytecode.pushInstruction(BytecodeInstructions.JUMP, (short) (s - bytecode.getInstructions().size()));
                            i.instruction.data[0] = (short) (bytecode.getInstructions().size() - pos);
                        }
                    }
                    continue;
                }
                //TODO: Not sure what this could be, maybe ++i or --i
                throw new RuntimeException("Cannot handle this token: " + token);
            }
        }
        return bytecode;
    }

    public static void convertOperationToBytecode(ArrayList<Scope> scopes, Bytecode bytecode, CompiledData data, Operation op, boolean returnRequired, Type t) {
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
            convertOperationToBytecode(scopes, bytecode, data, op.left, true, t);
        }else if(op instanceof OperationCall call) {
            if(call.reference.get(0).asWordToken().getWord().equals("syscall")) {
                int params = call.parameters.size();
                if(params > 7 || params <= 0) {
                    throw new FunctionNotDefinedException("syscall" + params);
                }

                for(int i = 0; i<call.parameters.size(); i++) {
                    convertOperationToBytecode(scopes, bytecode, data, call.parameters.get(i), true, Language.TYPES.get("uint64"));
                }

                if(params == 1) bytecode.pushInstruction(BytecodeInstructions.SYSCALL1);
                else if(params == 2) bytecode.pushInstruction(BytecodeInstructions.SYSCALL2);
                else if(params == 3) bytecode.pushInstruction(BytecodeInstructions.SYSCALL3);
                else if(params == 4) bytecode.pushInstruction(BytecodeInstructions.SYSCALL4);
                else if(params == 5) bytecode.pushInstruction(BytecodeInstructions.SYSCALL5);
                else if(params == 6) bytecode.pushInstruction(BytecodeInstructions.SYSCALL6);
                else if(params == 7) bytecode.pushInstruction(BytecodeInstructions.SYSCALL7);

                if(!returnRequired) {
                    bytecode.pushInstruction(BytecodeInstructions.POP64);
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
                    LocalVariable var = bytecode.getCurrentScope().getLocalVariable(f.reference.get(0).asWordToken().getWord());
                    parameters.add(var.type);
                }else {
                    throw new NotImplementedException("Not implemented yet");
                }
            }
            if(call.reference.size() != 1) {
                throw new NotImplementedException("Not implemented yet");
            }
            LocalVariable v = bytecode.getCurrentScope().findLocalVariable(call.reference.get(0).asWordToken().getWord());
            FunctionSignature ss = new FunctionSignature(null, parameters.toArray(new Type[0]));
            if(v == null) {
                Function func = data.getFunction(call.reference.get(0).asWordToken().getWord(), ss);
                short index = data.getFunctionIndex(func);
                for(int i = func.signature.parameters.length - 1; i >= 0; i--) {
                    convertOperationToBytecode(scopes, bytecode, data, call.parameters.get(i), true, func.signature.parameters[i]);
                }
                bytecode.pushInstruction(BytecodeInstructions.INVOKESTATIC, index);

                if(!returnRequired) {
                    int retSize = func.signature.returnType.getSize();
                    if(retSize == 0) ;
                    else if(retSize == 1) bytecode.pushInstruction(BytecodeInstructions.POP8);
                    else if(retSize == 2) bytecode.pushInstruction(BytecodeInstructions.POP16);
                    else if(retSize == 4) bytecode.pushInstruction(BytecodeInstructions.POP32);
                    else if(retSize == 8) bytecode.pushInstruction(BytecodeInstructions.POP64);
                    else throw new NotImplementedException("Size " + size + " is not supported");
                }
            }else {
                throw new NotImplementedException("Not implemented yet");
            }
        }else if(op instanceof OperationField) {
            SeekIterator<Token> tokens = new SeekIterator<>(((OperationField) op).reference);
            while(tokens.hasNext()) {
                Token token = tokens.next();
                if(token.getType() == TokenType.WORD) {
                    LocalVariable var = bytecode.getCurrentScope().findLocalVariable(token.asWordToken().getWord());
                    int psize = 0;
                    if(var != null) {
                        psize = var.type.getSize();
                        short index = (short) var.index;

                        if(psize == 1) bytecode.pushInstruction(BytecodeInstructions.LOAD8, index);
                        else if(psize == 2) bytecode.pushInstruction(BytecodeInstructions.LOAD16, index);
                        else if(psize == 4) bytecode.pushInstruction(BytecodeInstructions.LOAD32, index);
                        else if(psize == 8) bytecode.pushInstruction(BytecodeInstructions.LOAD64, index);
                    }else {
                        throw new NotImplementedException("Not implemented yet");
                    }

                    if(psize == 1) {
                        if(t.getSize() == 2) bytecode.pushInstruction(BytecodeInstructions.EX8T16);
                        else if(t.getSize() == 4) bytecode.pushInstruction(BytecodeInstructions.EX8T32);
                        else if(t.getSize() == 8) bytecode.pushInstruction(BytecodeInstructions.EX8T64);
                    } else if(psize == 2) {
                        if(t.getSize() == 1) bytecode.pushInstruction(BytecodeInstructions.EX16T8);
                        else if(t.getSize() == 4) bytecode.pushInstruction(BytecodeInstructions.EX16T32);
                        else if(t.getSize() == 8) bytecode.pushInstruction(BytecodeInstructions.EX16T64);
                    } else if(psize == 4) {
                        if(t.getSize() == 1) bytecode.pushInstruction(BytecodeInstructions.EX32T8);
                        else if(t.getSize() == 2) bytecode.pushInstruction(BytecodeInstructions.EX32T16);
                        else if(t.getSize() == 8) bytecode.pushInstruction(BytecodeInstructions.EX32T64);
                    } else if(psize == 8) {
                        if(t.getSize() == 1) bytecode.pushInstruction(BytecodeInstructions.EX64T8);
                        else if(t.getSize() == 2) bytecode.pushInstruction(BytecodeInstructions.EX64T16);
                        else if(t.getSize() == 4) bytecode.pushInstruction(BytecodeInstructions.EX64T32);
                    }

                }else if(token.getType() == TokenType.ACCESSOR) {
                    throw new NotImplementedException("Dereferencing is not implemented yet");
                }else {
                    throw new NotImplementedException("Cannot handle token: " + token);
                }
            }
        }else if(op instanceof OperationNumber) {
            BigInteger num = (((OperationNumber) op).number).number;
            if(numberInBounds(smallestNumber, biggestNumber, num)) {
                if(size == 1) bytecode.pushInstruction(BytecodeInstructions.PUSHI8, num.byteValue());
                else if(size == 2) bytecode.pushInstruction(BytecodeInstructions.PUSHI16, num.shortValue());
                else if(size == 4) bytecode.pushInstruction(BytecodeInstructions.PUSHI32, num.intValue());
                else if(size == 8) {
                    if(numberInBounds(BigInteger.ZERO, BigInteger.valueOf(0xff), num)) {
                        bytecode.pushInstruction(BytecodeInstructions.PUSHI64F8, num.byteValue());
                    }else {
                        bytecode.pushInstruction(BytecodeInstructions.PUSHI64, num.longValue());
                    }
                }
                else throw new NotImplementedException("Size " + size + " is not supported");
            }else {
                throw new RuntimeException("Number " + num + " is not in range of the type (" + smallestNumber + " to " + biggestNumber + ")");
            }
        }else if(op instanceof OperationString) {
            String str = ((OperationString) op).string.getString();
            short index = bytecode.addString(str);
            bytecode.pushInstruction(BytecodeInstructions.PUSHSTR, index);
        }else if(op instanceof OperationCast) {
            if(t == null) throw new NotImplementedException("Not supported cast");
            if(((OperationCast) op).type == null) throw new NotImplementedException("Not supported cast");
            convertOperationToBytecode(scopes, bytecode, data, op.left, true, ((OperationCast) op).type);

            Type t2 = ((OperationCast) op).type;
            int s1 = t2.getSize();
            int s2 = t.getSize();
            if(s1 == 1) {
                if(s2 == 2) bytecode.pushInstruction(BytecodeInstructions.EX8T16);
                else if(s2 == 4) bytecode.pushInstruction(BytecodeInstructions.EX8T32);
                else if(s2 == 8) bytecode.pushInstruction(BytecodeInstructions.EX8T64);
            }else if(s1 == 2) {
                if(s2 == 1) bytecode.pushInstruction(BytecodeInstructions.EX16T8);
                else if(s2 == 4) bytecode.pushInstruction(BytecodeInstructions.EX16T32);
                else if(s2 == 8) bytecode.pushInstruction(BytecodeInstructions.EX16T64);
            }else if(s1 == 4) {
                if(s2 == 1) bytecode.pushInstruction(BytecodeInstructions.EX32T8);
                else if(s2 == 2) bytecode.pushInstruction(BytecodeInstructions.EX32T16);
                else if(s2 == 8) bytecode.pushInstruction(BytecodeInstructions.EX32T64);
            }else if(s1 == 8) {
                if(s2 == 1) bytecode.pushInstruction(BytecodeInstructions.EX64T8);
                else if(s2 == 2) bytecode.pushInstruction(BytecodeInstructions.EX64T16);
                else if(s2 == 8) bytecode.pushInstruction(BytecodeInstructions.EX64T32);
            }else throw new NotImplementedException("Not supported cast");
        }else if(op instanceof OperationAssignment) {
            ArrayList<Token> tokens = ((OperationField) op.left).reference;

            try {
                LocalVariable lVar;

                if(tokens.size() == 1) {
                    lVar = bytecode.getCurrentScope().getLocalVariable(tokens.get(0).asWordToken().getWord());
                } else {
                    throw new NotImplementedException("TODO");
                }
                convertOperationToBytecode(scopes, bytecode, data, op.right, true, lVar.type);

                short index = (short) lVar.index;
                int psize = lVar.type.getSize();
                if(psize == 1) bytecode.pushInstruction(BytecodeInstructions.STORE8, index);
                else if(psize == 2) bytecode.pushInstruction(BytecodeInstructions.STORE16, index);
                else if(psize == 4) bytecode.pushInstruction(BytecodeInstructions.STORE32, index);
                else if(psize == 8) bytecode.pushInstruction(BytecodeInstructions.STORE64, index);
                else throw new NotImplementedException("Size " + psize + " is not supported");
            }catch(VariableNotDefinedException e) {
                ObjectField var;

                if(tokens.size() == 1) {
                    var = data.getField(tokens.get(0).asWordToken().getWord());
                    if(var.flags.contains(Flag.FINAL)) {
                        throw new RuntimeException("Cannot change contents of a final variable: " + var.name);
                    }
                } else {
                    throw new NotImplementedException("TODO");
                }
                convertOperationToBytecode(scopes, bytecode, data, op.right, true, var.type);

                bytecode.pushSizedInstruction("SETSTATICFIELD", var.type.getSize(), data.getFieldIndex(tokens.get(0).asWordToken().getWord()));
            }

        }else if(op instanceof OperationComparison) {
            convertOperationToBytecode(scopes, bytecode, data, op.left, true, Language.TYPES.get("uint64"));
            convertOperationToBytecode(scopes, bytecode, data, op.right, true, Language.TYPES.get("uint64"));
            bytecode.pushInstruction(BytecodeInstructions.COMPARE64);
        }else if(op instanceof OperationComparisonNot) {
            convertOperationToBytecode(scopes, bytecode, data, op.left, true, Language.TYPES.get("uint64"));
            convertOperationToBytecode(scopes, bytecode, data, op.right, true, Language.TYPES.get("uint64"));
            bytecode.pushInstruction(BytecodeInstructions.COMPAREN64);
        }else {
            convertOperationToBytecode(scopes, bytecode, data, op.left, true, t);
            convertOperationToBytecode(scopes, bytecode, data, op.right, true, t);
            if(op instanceof OperationAdd) {
                bytecode.pushSizedInstruction("ADD", size);
            }else if(op instanceof OperationSubtract) {
                bytecode.pushSizedInstruction("SUB", size);
            }else if(op instanceof OperationDivide) {
                if(unsigned) bytecode.pushSizedInstruction("DIVU", size);
                else bytecode.pushSizedInstruction("DIVS", size);
            }else if(op instanceof OperationMultiply) {
                if(unsigned) bytecode.pushSizedInstruction("MULU", size);
                else bytecode.pushSizedInstruction("MULS", size);
            }else if(op instanceof OperationMod) {
                if(unsigned) bytecode.pushSizedInstruction("MUDU", size);
                else bytecode.pushSizedInstruction("MUDS", size);
            }else if(op instanceof OperationAnd) {
                bytecode.pushSizedInstruction("AND", size);
            }else if(op instanceof OperationShiftLeft) {
                bytecode.pushSizedInstruction("SHL", size);
            }else if(op instanceof OperationShiftRight) {
                bytecode.pushSizedInstruction("SHR", size);
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
