package ga.epicpix.zprol;

import ga.epicpix.zprol.compiled.CompileOperation;
import ga.epicpix.zprol.compiled.CompileOperationType;
import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.compiled.Flag;
import ga.epicpix.zprol.compiled.Function;
import ga.epicpix.zprol.compiled.Scope;
import ga.epicpix.zprol.compiled.TypeFunctionSignature;
import ga.epicpix.zprol.compiled.Types;
import ga.epicpix.zprol.compiled.bytecode.Bytecode;
import ga.epicpix.zprol.compiled.LocalVariable;
import ga.epicpix.zprol.compiled.bytecode.BytecodeInstructions;
import ga.epicpix.zprol.compiled.operation.Operation.OperationAdd;
import ga.epicpix.zprol.compiled.operation.Operation.OperationAnd;
import ga.epicpix.zprol.compiled.operation.Operation.OperationAssignment;
import ga.epicpix.zprol.compiled.operation.Operation.OperationBrackets;
import ga.epicpix.zprol.compiled.operation.Operation.OperationCall;
import ga.epicpix.zprol.compiled.operation.Operation.OperationCast;
import ga.epicpix.zprol.compiled.operation.Operation.OperationComparison;
import ga.epicpix.zprol.compiled.operation.Operation.OperationComparisonNot;
import ga.epicpix.zprol.compiled.operation.Operation.OperationDivide;
import ga.epicpix.zprol.compiled.operation.Operation.OperationField;
import ga.epicpix.zprol.compiled.operation.Operation.OperationMod;
import ga.epicpix.zprol.compiled.operation.Operation.OperationMultiply;
import ga.epicpix.zprol.compiled.operation.Operation.OperationNumber;
import ga.epicpix.zprol.compiled.operation.Operation.OperationShiftLeft;
import ga.epicpix.zprol.compiled.operation.Operation.OperationShiftRight;
import ga.epicpix.zprol.compiled.operation.Operation.OperationString;
import ga.epicpix.zprol.compiled.operation.Operation.OperationSubtract;
import ga.epicpix.zprol.compiled.operation.OperationCompiler;
import ga.epicpix.zprol.compiled.Object;
import ga.epicpix.zprol.compiled.ObjectField;
import ga.epicpix.zprol.compiled.Structure;
import ga.epicpix.zprol.compiled.StructureField;
import ga.epicpix.zprol.compiled.Type;
import ga.epicpix.zprol.compiled.TypeFunctionSignatureNamed;
import ga.epicpix.zprol.compiled.TypeNamed;
import ga.epicpix.zprol.compiled.operation.Operation;
import ga.epicpix.zprol.exceptions.FunctionNotDefinedException;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.exceptions.UnknownTypeException;
import ga.epicpix.zprol.exceptions.VariableNotDefinedException;
import ga.epicpix.zprol.tokens.FieldToken;
import ga.epicpix.zprol.tokens.FunctionToken;
import ga.epicpix.zprol.tokens.ObjectToken;
import ga.epicpix.zprol.tokens.OperatorToken;
import ga.epicpix.zprol.tokens.StructureToken;
import ga.epicpix.zprol.tokens.Token;
import ga.epicpix.zprol.tokens.TokenType;
import ga.epicpix.zprol.tokens.TypedefToken;
import ga.epicpix.zprol.tokens.WordToken;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Stack;

public class Compiler {

    public static Bytecode parseFunctionCode(ArrayList<Scope> scopes, CompiledData data, SeekIterator<Token> tokens, TypeFunctionSignatureNamed sig) {
        OperationCompiler mathCompiler = new OperationCompiler();
        Bytecode bytecode = new Bytecode();
        for(TypeNamed param : sig.parameters) {
            bytecode.getCurrentScope().defineLocalVariable(param.name, param.type);
        }
        int opens = 0;
        Stack<CompileOperation> operations = new Stack<>();
        Token token;
        while((token = tokens.next()).getType() != TokenType.END_FUNCTION || opens != 0) {
            if(token.getType() == TokenType.WORD) {
                WordToken w = (WordToken) token;
                if(w.word.equals("return")) {
                    if(sig.returnType.type.memorySize == 0) {
                        if(tokens.next().getType() != TokenType.END_LINE) {
                            throw new RuntimeException("Missing ';'");
                        }
                        bytecode.pushInstruction(BytecodeInstructions.RETURN);
                    }else {
                        mathCompiler.reset();
                        convertOperationToBytecode(scopes, sig.returnType.type, bytecode, data, mathCompiler.compile(data, bytecode, tokens), true, sig.returnType);
                        int size = sig.returnType.type.memorySize;
                        if(size == 1) bytecode.pushInstruction(BytecodeInstructions.RETURN8);
                        else if(size == 2) bytecode.pushInstruction(BytecodeInstructions.RETURN16);
                        else if(size == 4) bytecode.pushInstruction(BytecodeInstructions.RETURN32);
                        else if(size == 8) bytecode.pushInstruction(BytecodeInstructions.RETURN64);
                        else throw new RuntimeException("Size " + size + " is not supported");
                    }
                }else if(w.word.equals("if")) {
                    if(tokens.next().getType() != TokenType.OPEN) {
                        throw new RuntimeException("Missing '('");
                    }
                    ArrayList<Operation> op = new ArrayList<>();
                    mathCompiler.reset();
                    mathCompiler.compile0(1, op, new Stack<>(), tokens, data);
                    convertOperationToBytecode(scopes, Types.BOOLEAN, bytecode, data, op.get(0), tokens.seek().getType() == TokenType.OPEN, null);
                    if(tokens.seek().getType() == TokenType.OPEN) {
                        operations.push(new CompileOperation(bytecode.pushInstruction(BytecodeInstructions.JUMPNE, (short) bytecode.getInstructions().size()), CompileOperationType.IF));
                    }else if(tokens.seek().getType() != TokenType.CLOSE) {
                        throw new RuntimeException("Unknown symbol: '" + tokens.seek() + "'");
                    }
                }else if(w.word.equals("while")) {
                    if(tokens.next().getType() != TokenType.OPEN) {
                        throw new RuntimeException("Missing '('");
                    }
                    ArrayList<Operation> op = new ArrayList<>();
                    mathCompiler.reset();
                    mathCompiler.compile0(1, op, new Stack<>(), tokens, data);
                    int s = bytecode.getInstructions().size();
                    convertOperationToBytecode(scopes, Types.BOOLEAN, bytecode, data, op.get(0), tokens.seek().getType() == TokenType.OPEN, null);
                    if(tokens.seek().getType() == TokenType.OPEN) {
                        operations.push(new CompileOperation(bytecode.pushInstruction(BytecodeInstructions.JUMPNE, (short) s, (short) bytecode.getInstructions().size()), CompileOperationType.WHILE));
                    }else if(tokens.seek().getType() != TokenType.CLOSE) {
                        throw new RuntimeException("Unknown symbol: '" + tokens.seek() + "'");
                    }
                }else {
                    int startIndex = tokens.currentIndex() - 1;
                    try {
                        tokens.back();
                        Type type = data.resolveType(tokens);
                        token = tokens.next();
                        if(token.getType() != TokenType.WORD) {
                            throw new RuntimeException("Cannot handle this token: " + token);
                        }
                        String name = ((WordToken) token).word;
                        LocalVariable lVar = bytecode.getCurrentScope().defineLocalVariable(name, type);
                        token = tokens.next();
                        if(token.getType() != TokenType.END_LINE) {
                            if(token.getType() == TokenType.OPERATOR) {
                                if(((OperatorToken) token).operator.equals("=")) {
                                    mathCompiler.reset();
                                    convertOperationToBytecode(scopes, type.type, bytecode, data, mathCompiler.compile(data, bytecode, tokens), true, type);
                                    int size = type.type.memorySize;
                                    short index = (short) lVar.index;
                                    if(size == 1) bytecode.pushInstruction(BytecodeInstructions.STORE8, index);
                                    else if(size == 2) bytecode.pushInstruction(BytecodeInstructions.STORE16, index);
                                    else if(size == 4) bytecode.pushInstruction(BytecodeInstructions.STORE32, index);
                                    else if(size == 8) bytecode.pushInstruction(BytecodeInstructions.STORE64, index);
                                    else throw new RuntimeException("Size " + size + " is not supported");
                                }else {
                                    throw new RuntimeException("Cannot handle this token: " + token);
                                }
                            }else {
                                throw new RuntimeException("Cannot handle this token: " + token);
                            }
                        }
                    } catch (UnknownTypeException unkType) {
                        tokens.setIndex(startIndex);
                        mathCompiler.reset();
                        convertOperationToBytecode(scopes, null, bytecode, data, mathCompiler.compile(data, bytecode, tokens), false, null);
                    }
                }
            }else {
                if(token.getType() == TokenType.OPEN) {
                    opens++;
                    bytecode.newScope();
                    continue;
                }else if(token.getType() == TokenType.CLOSE) {
                    opens--;
                    bytecode.leaveScope();
                    if(operations.size() != 0) {
                        CompileOperation i = operations.pop();
                        short s = (short) i.instruction.data[0];
                        i.instruction.data[0] = (short) (bytecode.getInstructions().size() - s);
                        if(i.type == CompileOperationType.IF) {
                            if(tokens.seek().getType() == TokenType.WORD && ((WordToken) tokens.seek()).word.equals("else")) {
                                tokens.next();
                                Token w = tokens.seek();
                                if(w instanceof WordToken && ((WordToken) w).word.equals("if")) {
                                    tokens.next();
                                    if(tokens.next().getType() != TokenType.OPEN) {
                                        throw new RuntimeException("Missing '('");
                                    }
                                    ArrayList<Operation> op = new ArrayList<>();
                                    mathCompiler.reset();
                                    mathCompiler.compile0(1, op, new Stack<>(), tokens, data);
                                    convertOperationToBytecode(scopes, Types.BOOLEAN, bytecode, data, op.get(0), tokens.seek().getType() == TokenType.OPEN, null);
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

    public static void convertOperationToBytecode(ArrayList<Scope> scopes, Types type, Bytecode bytecode, CompiledData data, Operation op, boolean returnRequired, Type t) {
        int size = 0;
        boolean unsigned = false;
        BigInteger biggestNumber = BigInteger.ZERO;
        BigInteger smallestNumber = BigInteger.ZERO;

        if(type != null && type.isNumberType()) {
            size = type.memorySize;
            unsigned = type.isUnsignedNumber();
            if(unsigned) {
                biggestNumber = BigDecimal.valueOf(Math.pow(2, size * 8) - 1).toBigInteger();
            } else {
                biggestNumber = BigDecimal.valueOf(Math.pow(2, size * 8 - 1) - 1).toBigInteger();
                smallestNumber = BigDecimal.valueOf(-Math.pow(2, size * 8 - 1)).toBigInteger();
            }
        }

        if(op instanceof OperationBrackets) {
            convertOperationToBytecode(scopes, type, bytecode, data, op.left, true, t);
        }else if(op instanceof OperationCall) {
            OperationCall call = (OperationCall) op;
            if(((WordToken) call.reference.get(0)).word.equals("syscall")) {
                int params = call.parameters.size();
                if(params > 7 || params <= 0) {
                    throw new FunctionNotDefinedException("syscall" + params);
                }

                for(int i = 0; i<call.parameters.size(); i++) {
                    convertOperationToBytecode(scopes, Types.UINT64, bytecode, data, call.parameters.get(i), true, null);
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
                if(param instanceof OperationString) {
                    parameters.add(new Type(Types.POINTER));
                }else if(param instanceof OperationField) {
                    OperationField f = (OperationField) param;
                    if(f.reference.size() != 1) {
                        throw new NotImplementedException("Not implemented yet");
                    }
                    LocalVariable var = bytecode.getCurrentScope().getLocalVariable(((WordToken) f.reference.get(0)).word);
                    parameters.add(var.type);
                }else {
                    parameters.add(new Type(Types.NUMBER));
                }
            }
            if(call.reference.size() != 1) {
                throw new NotImplementedException("Not implemented yet");
            }
            LocalVariable v = bytecode.getCurrentScope().findLocalVariable(((WordToken) call.reference.get(0)).word);
            TypeFunctionSignature ss = new TypeFunctionSignature(null, parameters.toArray(new Type[0]));
            if(v == null) {
                Function func = data.getFunction(((WordToken) call.reference.get(0)).word, ss);
                ArrayList<Function> functions = data.getFunctions();
                short index = -1;
                for(int i = 0; i < functions.size(); i++) {
                    if(functions.get(i) == func) {
                        index = (short) i;
                        break;
                    }
                }
                for(int i = func.signature.parameters.length - 1; i >= 0; i--) {
                    convertOperationToBytecode(scopes, func.signature.parameters[i].type.type, bytecode, data, call.parameters.get(i), true, func.signature.parameters[i].type);
                }
                bytecode.pushInstruction(BytecodeInstructions.INVOKESTATIC, index);

                if(!returnRequired) {
                    int retSize = func.signature.returnType.type.memorySize;
                    if(retSize == 0) ;
                    else if(retSize == 1) bytecode.pushInstruction(BytecodeInstructions.POP8);
                    else if(retSize == 2) bytecode.pushInstruction(BytecodeInstructions.POP16);
                    else if(retSize == 4) bytecode.pushInstruction(BytecodeInstructions.POP32);
                    else if(retSize == 8) bytecode.pushInstruction(BytecodeInstructions.POP64);
                    else throw new NotImplementedException("Size " + size + " is not supported");
                }
            }else {
                if(v.type.type == Types.FUNCTION_SIGNATURE) {
                    TypeFunctionSignature sig = (TypeFunctionSignature) v.type;
                    if(sig.validateFunctionSignature(ss)) {
                        for(int i = sig.parameters.length - 1; i >= 0; i--) {
                            convertOperationToBytecode(scopes, sig.parameters[i].type, bytecode, data, call.parameters.get(i), true, sig.parameters[i]);
                        }
                        bytecode.pushInstruction(BytecodeInstructions.LOAD64, (short) v.index);
                        bytecode.pushInstruction(BytecodeInstructions.INVOKESIGNATURE, sig);

                        if(!returnRequired) {
                            int retSize = sig.returnType.type.memorySize;
                            if(retSize == 0) ;
                            else if(retSize == 1) bytecode.pushInstruction(BytecodeInstructions.POP8);
                            else if(retSize == 2) bytecode.pushInstruction(BytecodeInstructions.POP16);
                            else if(retSize == 4) bytecode.pushInstruction(BytecodeInstructions.POP32);
                            else if(retSize == 8) bytecode.pushInstruction(BytecodeInstructions.POP64);
                            else throw new NotImplementedException("Size " + size + " is not supported");
                        }
                    }else {
                        throw new IllegalArgumentException("Function signatures do not match!");
                    }
                }else {
                    throw new IllegalArgumentException("Cannot call not a function!");
                }
            }
        }else if(op instanceof OperationField) {
            SeekIterator<Token> tokens = new SeekIterator<>(((OperationField) op).reference);
            while(tokens.hasNext()) {
                Token token = tokens.next();
                if(token.getType() == TokenType.WORD) {
                    LocalVariable var = bytecode.getCurrentScope().findLocalVariable(((WordToken) token).word);
                    int psize = 0;
                    if(var != null) {
                        psize = var.type.type.memorySize;
                        short index = (short) var.index;

                        if(psize == 1) bytecode.pushInstruction(BytecodeInstructions.LOAD8, index);
                        else if(psize == 2) bytecode.pushInstruction(BytecodeInstructions.LOAD16, index);
                        else if(psize == 4) bytecode.pushInstruction(BytecodeInstructions.LOAD32, index);
                        else if(psize == 8) bytecode.pushInstruction(BytecodeInstructions.LOAD64, index);
                    }else {
                        if(t.type == Types.FUNCTION_SIGNATURE) {
                            Function func = data.getFunction(((WordToken) token).word, (TypeFunctionSignature) t);
                            ArrayList<Function> functions = data.getFunctions();
                            short index = -1;
                            for(int i = 0; i < functions.size(); i++) {
                                if(functions.get(i) == func) {
                                    index = (short) i;
                                    break;
                                }
                            }
                            bytecode.pushInstruction(BytecodeInstructions.PUSHFUNCTION, index);
                            continue;
                        }else {
                            ObjectField ovar = data.getField(((WordToken) token).word);
                            psize = ovar.type.type.memorySize;
                            short index = data.getFieldIndex(((WordToken) token).word);

                            if(psize == 1) bytecode.pushInstruction(BytecodeInstructions.GETSTATICFIELD8, index);
                            else if(psize == 2) bytecode.pushInstruction(BytecodeInstructions.GETSTATICFIELD16, index);
                            else if(psize == 4) bytecode.pushInstruction(BytecodeInstructions.GETSTATICFIELD32, index);
                            else if(psize == 8) bytecode.pushInstruction(BytecodeInstructions.GETSTATICFIELD64, index);
                        }
                    }

                    if(psize == 1) {
                        if(type.memorySize == 2) bytecode.pushInstruction(BytecodeInstructions.EX8T16);
                        else if(type.memorySize == 4) bytecode.pushInstruction(BytecodeInstructions.EX8T32);
                        else if(type.memorySize == 8) bytecode.pushInstruction(BytecodeInstructions.EX8T64);
                    } else if(psize == 2) {
                        if(type.memorySize == 1) bytecode.pushInstruction(BytecodeInstructions.EX16T8);
                        else if(type.memorySize == 4) bytecode.pushInstruction(BytecodeInstructions.EX16T32);
                        else if(type.memorySize == 8) bytecode.pushInstruction(BytecodeInstructions.EX16T64);
                    } else if(psize == 4) {
                        if(type.memorySize == 1) bytecode.pushInstruction(BytecodeInstructions.EX32T8);
                        else if(type.memorySize == 2) bytecode.pushInstruction(BytecodeInstructions.EX32T16);
                        else if(type.memorySize == 8) bytecode.pushInstruction(BytecodeInstructions.EX32T64);
                    } else if(psize == 8) {
                        if(type.memorySize == 1) bytecode.pushInstruction(BytecodeInstructions.EX64T8);
                        else if(type.memorySize == 2) bytecode.pushInstruction(BytecodeInstructions.EX64T16);
                        else if(type.memorySize == 4) bytecode.pushInstruction(BytecodeInstructions.EX64T32);
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
            convertOperationToBytecode(scopes, ((OperationCast) op).type.type, bytecode, data, op.left, true, ((OperationCast) op).type);

            Type t2 = ((OperationCast) op).type;
            int s1 = t2.type.memorySize;
            int s2 = t.type.memorySize;
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
                    lVar = bytecode.getCurrentScope().getLocalVariable(((WordToken) tokens.get(0)).word);
                } else {
                    throw new NotImplementedException("TODO");
                }
                convertOperationToBytecode(scopes, lVar.type.type, bytecode, data, op.right, true, lVar.type);

                short index = (short) lVar.index;
                int psize = lVar.type.type.memorySize;
                if(psize == 1) bytecode.pushInstruction(BytecodeInstructions.STORE8, index);
                else if(psize == 2) bytecode.pushInstruction(BytecodeInstructions.STORE16, index);
                else if(psize == 4) bytecode.pushInstruction(BytecodeInstructions.STORE32, index);
                else if(psize == 8) bytecode.pushInstruction(BytecodeInstructions.STORE64, index);
                else throw new NotImplementedException("Size " + psize + " is not supported");
            }catch(VariableNotDefinedException e) {
                ObjectField var;

                if(tokens.size() == 1) {
                    var = data.getField(((WordToken) tokens.get(0)).word);
                    if(var.flags.contains(Flag.FINAL)) {
                        throw new RuntimeException("Cannot change contents of a final variable: " + var.name);
                    }
                } else {
                    throw new NotImplementedException("TODO");
                }
                convertOperationToBytecode(scopes, var.type.type, bytecode, data, op.right, true, var.type);


                short index = data.getFieldIndex(((WordToken) tokens.get(0)).word);
                int psize = var.type.type.memorySize;
                if(psize == 1) bytecode.pushInstruction(BytecodeInstructions.SETSTATICFIELD8, index);
                else if(psize == 2) bytecode.pushInstruction(BytecodeInstructions.SETSTATICFIELD16, index);
                else if(psize == 4) bytecode.pushInstruction(BytecodeInstructions.SETSTATICFIELD32, index);
                else if(psize == 8) bytecode.pushInstruction(BytecodeInstructions.SETSTATICFIELD64, index);
                else throw new NotImplementedException("Size " + psize + " is not supported");
            }

        }else if(op instanceof OperationComparison) {
            convertOperationToBytecode(scopes, Types.UINT64, bytecode, data, op.left, true, null);
            convertOperationToBytecode(scopes, Types.UINT64, bytecode, data, op.right, true, null);
            bytecode.pushInstruction(BytecodeInstructions.COMPARE64);
        }else if(op instanceof OperationComparisonNot) {
            convertOperationToBytecode(scopes, Types.UINT64, bytecode, data, op.left, true, null);
            convertOperationToBytecode(scopes, Types.UINT64, bytecode, data, op.right, true, null);
            bytecode.pushInstruction(BytecodeInstructions.COMPAREN64);
        }else {
            convertOperationToBytecode(scopes, type, bytecode, data, op.left, true, t);
            convertOperationToBytecode(scopes, type, bytecode, data, op.right, true, t);
            if(op instanceof OperationAdd) {
                if(size == 1) bytecode.pushInstruction(BytecodeInstructions.ADD8);
                else if(size == 2) bytecode.pushInstruction(BytecodeInstructions.ADD16);
                else if(size == 4) bytecode.pushInstruction(BytecodeInstructions.ADD32);
                else if(size == 8) bytecode.pushInstruction(BytecodeInstructions.ADD64);
                else throw new NotImplementedException("Size " + size + " is not supported");
            }else if(op instanceof OperationSubtract) {
                if(size == 1) bytecode.pushInstruction(BytecodeInstructions.SUB8);
                else if(size == 2) bytecode.pushInstruction(BytecodeInstructions.SUB16);
                else if(size == 4) bytecode.pushInstruction(BytecodeInstructions.SUB32);
                else if(size == 8) bytecode.pushInstruction(BytecodeInstructions.SUB64);
                else throw new NotImplementedException("Size " + size + " is not supported");
            }else if(op instanceof OperationDivide) {
                if(size == 1 && unsigned) bytecode.pushInstruction(BytecodeInstructions.DIVU8);
                else if(size == 2 && unsigned) bytecode.pushInstruction(BytecodeInstructions.DIVU16);
                else if(size == 4 && unsigned) bytecode.pushInstruction(BytecodeInstructions.DIVU32);
                else if(size == 8 && unsigned) bytecode.pushInstruction(BytecodeInstructions.DIVU64);
                else if(size == 1 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.DIVS8);
                else if(size == 2 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.DIVS16);
                else if(size == 4 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.DIVS32);
                else if(size == 8 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.DIVS64);
                else throw new NotImplementedException("Size " + size + " is not supported");
            }else if(op instanceof OperationMultiply) {
                if(size == 1 && unsigned) bytecode.pushInstruction(BytecodeInstructions.MULU8);
                else if(size == 2 && unsigned) bytecode.pushInstruction(BytecodeInstructions.MULU16);
                else if(size == 4 && unsigned) bytecode.pushInstruction(BytecodeInstructions.MULU32);
                else if(size == 8 && unsigned) bytecode.pushInstruction(BytecodeInstructions.MULU64);
                else if(size == 1 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.MULS8);
                else if(size == 2 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.MULS16);
                else if(size == 4 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.MULS32);
                else if(size == 8 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.MULS64);
                else throw new NotImplementedException("Size " + size + " is not supported");
            }else if(op instanceof OperationMod) {
                if(size == 1 && unsigned) bytecode.pushInstruction(BytecodeInstructions.MODU8);
                else if(size == 2 && unsigned) bytecode.pushInstruction(BytecodeInstructions.MODU16);
                else if(size == 4 && unsigned) bytecode.pushInstruction(BytecodeInstructions.MODU32);
                else if(size == 8 && unsigned) bytecode.pushInstruction(BytecodeInstructions.MODU64);
                else if(size == 1 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.MODS8);
                else if(size == 2 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.MODS16);
                else if(size == 4 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.MODS32);
                else if(size == 8 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.MODS64);
                else throw new NotImplementedException("Size " + size + " is not supported");
            }else if(op instanceof OperationAnd) {
                if(size == 1) bytecode.pushInstruction(BytecodeInstructions.AND8);
                else if(size == 2) bytecode.pushInstruction(BytecodeInstructions.AND16);
                else if(size == 4) bytecode.pushInstruction(BytecodeInstructions.AND32);
                else if(size == 8) bytecode.pushInstruction(BytecodeInstructions.AND64);
                else throw new NotImplementedException("Size " + size + " is not supported");
            }else if(op instanceof OperationShiftLeft) {
                if(size == 1) bytecode.pushInstruction(BytecodeInstructions.SHL8);
                else if(size == 2) bytecode.pushInstruction(BytecodeInstructions.SHL16);
                else if(size == 4) bytecode.pushInstruction(BytecodeInstructions.SHL32);
                else if(size == 8) bytecode.pushInstruction(BytecodeInstructions.SHL64);
                else throw new NotImplementedException("Size " + size + " is not supported");
            }else if(op instanceof OperationShiftRight) {
                if(size == 1) bytecode.pushInstruction(BytecodeInstructions.SHR8);
                else if(size == 2) bytecode.pushInstruction(BytecodeInstructions.SHR16);
                else if(size == 4) bytecode.pushInstruction(BytecodeInstructions.SHR32);
                else if(size == 8) bytecode.pushInstruction(BytecodeInstructions.SHR64);
                else throw new NotImplementedException("Size " + size + " is not supported");
            }else {
                throw new NotImplementedException(op.getClass().getSimpleName() + " is not implemented!");
            }
        }
    }

    private static boolean numberInBounds(BigInteger lowest, BigInteger highest, BigInteger num) {
        return num.compareTo(lowest) >= 0 && highest.compareTo(num) >= 0;
    }

    public static Function compileFunction(ArrayList<Scope> scopes, CompiledData data, FunctionToken functionToken, SeekIterator<Token> tokens) throws UnknownTypeException {
        ArrayList<Flag> flags = convertFlags(functionToken.flags);
        Type returnType = data.resolveType(functionToken.returnType);
        ArrayList<TypeNamed> parameters = new ArrayList<>();
        for(ParameterDataType param : functionToken.parameters) {
            parameters.add(new TypeNamed(data.resolveType(param.type), param.name));
        }
        TypeFunctionSignatureNamed signature = new TypeFunctionSignatureNamed(returnType, parameters.toArray(new TypeNamed[0]));
        if(flags.contains(Flag.NO_IMPLEMENTATION)) {
            return new Function(functionToken.name, signature, flags, null);
        }
        return new Function(functionToken.name, signature, flags, parseFunctionCode(scopes, data, tokens, signature));
    }

    public static Structure compileStructure(ArrayList<Scope> scopes, CompiledData data, StructureToken structureToken, SeekIterator<Token> tokens) throws UnknownTypeException {
        ArrayList<StructureField> fields = new ArrayList<>();
        for(StructureType field : structureToken.getTypes()) {
            fields.add(new StructureField(field.name, data.resolveType(field.type)));
        }
        return new Structure(structureToken.getStructureName(), fields);
    }

    public static ArrayList<Flag> convertFlags(ArrayList<ParserFlag> pFlags) {
        ArrayList<Flag> flags = new ArrayList<>();
        for(ParserFlag parserFlag : pFlags) {
            if(parserFlag == ParserFlag.INTERNAL) {
                flags.add(Flag.INTERNAL);
            }else if(parserFlag == ParserFlag.NO_IMPLEMENTATION) {
                flags.add(Flag.NO_IMPLEMENTATION);
            }else if(parserFlag == ParserFlag.STATIC) {
                flags.add(Flag.STATIC);
            }else if(parserFlag == ParserFlag.FINAL) {
                flags.add(Flag.FINAL);
            }
        }
        return flags;
    }

    public static ObjectField compileField(ArrayList<Scope> scopes, CompiledData data, FieldToken field, boolean primary) throws UnknownTypeException {
        Type type = data.resolveType(field.type);
        if(primary) {
            Function func = data.getInitFunction();
            Bytecode bytecode = func.code;
            if(field.ops.size() != 1) {
                Operation op = new OperationCompiler().compile(data, bytecode, new SeekIterator<>(field.ops));
                convertOperationToBytecode(scopes, type.type, bytecode, data, op, true, type);
                int psize = type.type.memorySize;
                if(psize == 1) bytecode.pushInstruction(BytecodeInstructions.SETSTATICFIELD8, (short) data.getFields().size());
                else if(psize == 2) bytecode.pushInstruction(BytecodeInstructions.SETSTATICFIELD16, (short) data.getFields().size());
                else if(psize == 4) bytecode.pushInstruction(BytecodeInstructions.SETSTATICFIELD32, (short) data.getFields().size());
                else if(psize == 8) bytecode.pushInstruction(BytecodeInstructions.SETSTATICFIELD64, (short) data.getFields().size());
                else throw new NotImplementedException("Size " + psize + " is not supported");
            }
        }
        return new ObjectField(field.name, type, convertFlags(field.flags));
    }

    public static Object compileObject(ArrayList<Scope> scopes, CompiledData data, ObjectToken objectToken, SeekIterator<Token> tokens) throws UnknownTypeException {
        ArrayList<ObjectField> fields = new ArrayList<>();
        ArrayList<Function> functions = new ArrayList<>();
        Token currentToken;
        while((currentToken = tokens.next()).getType() != TokenType.END_OBJECT) {
            if(currentToken.getType() == TokenType.FIELD) {
                FieldToken fieldToken = (FieldToken) currentToken;
                fields.add(compileField(scopes, data, fieldToken, false));
            }else if(currentToken.getType() == TokenType.FUNCTION) {
                functions.add(compileFunction(scopes, data, (FunctionToken) currentToken, tokens));
            }
        }
        return new Object(objectToken.getObjectName(), data.resolveType(objectToken.getExtendsFrom()), fields, functions);
    }

    public static CompiledData compile(ArrayList<Token> tokens) throws UnknownTypeException {
        CompiledData data = new CompiledData();
        for(Token token : tokens) {
            if(token.getType() == TokenType.STRUCTURE) {
                data.addFutureStructureDefinition(((StructureToken) token).getStructureName());
            }else if(token.getType() == TokenType.OBJECT) {
                data.addFutureObjectDefinition(((ObjectToken) token).getObjectName());
            }
        }
        ArrayList<Scope> scopes = new ArrayList<>();

        SeekIterator<Token> tokenIter = new SeekIterator<>(tokens);
        while(tokenIter.hasNext()) {
            Token token = tokenIter.next();
            if(token.getType() == TokenType.STRUCTURE) {
                data.addStructure(compileStructure(scopes, data, (StructureToken) token, tokenIter));
            }else if(token.getType() == TokenType.OBJECT) {
                data.addObject(compileObject(scopes, data, (ObjectToken) token, tokenIter));
            }else if(token.getType() == TokenType.FUNCTION) {
                data.addFunction(compileFunction(scopes, data, (FunctionToken) token, tokenIter));
            }else if(token.getType() == TokenType.FIELD) {
                data.addField(compileField(scopes, data, (FieldToken) token, true));
            }else if(token.getType() == TokenType.TYPEDEF) {
                TypedefToken typedefToken = (TypedefToken) token;
                data.addTypeDefinition(typedefToken.getToType(), data.resolveType(typedefToken.getFromType()));
            }
        }
        data.finishFutures();
        return data;
    }

}
