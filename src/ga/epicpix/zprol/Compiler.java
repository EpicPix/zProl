package ga.epicpix.zprol;

import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.compiled.Flag;
import ga.epicpix.zprol.compiled.Function;
import ga.epicpix.zprol.compiled.bytecode.Bytecode;
import ga.epicpix.zprol.compiled.LocalVariable;
import ga.epicpix.zprol.compiled.bytecode.BytecodeInstructions;
import ga.epicpix.zprol.compiled.math.MathAdd;
import ga.epicpix.zprol.compiled.math.MathAnd;
import ga.epicpix.zprol.compiled.math.MathBrackets;
import ga.epicpix.zprol.compiled.math.MathCall;
import ga.epicpix.zprol.compiled.math.MathCompiler;
import ga.epicpix.zprol.compiled.Object;
import ga.epicpix.zprol.compiled.ObjectField;
import ga.epicpix.zprol.compiled.Structure;
import ga.epicpix.zprol.compiled.StructureField;
import ga.epicpix.zprol.compiled.Type;
import ga.epicpix.zprol.compiled.TypeFunctionSignatureNamed;
import ga.epicpix.zprol.compiled.TypeNamed;
import ga.epicpix.zprol.compiled.math.MathDivide;
import ga.epicpix.zprol.compiled.math.MathField;
import ga.epicpix.zprol.compiled.math.MathMod;
import ga.epicpix.zprol.compiled.math.MathMultiply;
import ga.epicpix.zprol.compiled.math.MathNumber;
import ga.epicpix.zprol.compiled.math.MathOperation;
import ga.epicpix.zprol.compiled.math.MathShiftLeft;
import ga.epicpix.zprol.compiled.math.MathShiftRight;
import ga.epicpix.zprol.compiled.math.MathSubtract;
import ga.epicpix.zprol.exceptions.UnknownTypeException;
import ga.epicpix.zprol.tokens.FieldToken;
import ga.epicpix.zprol.tokens.FunctionToken;
import ga.epicpix.zprol.tokens.NumberToken;
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

public class Compiler {

    public static Bytecode parseFunctionCode(CompiledData data, SeekIterator<Token> tokens) {
        MathCompiler mathCompiler = new MathCompiler();
        Bytecode bytecode = new Bytecode();
        Token token;
        while((token = tokens.next()).getType() != TokenType.END_FUNCTION) {
            if(token.getType() == TokenType.WORD) {
                try {
                    Type type = data.resolveType(((WordToken) token).word);
                    token = tokens.next();
                    if(token.getType() != TokenType.WORD) {
                        throw new RuntimeException("Cannot handle this token: " + token);
                    }
                    String name = ((WordToken) token).word;
                    bytecode.defineLocalVariable(name, type);
                    token = tokens.next();
                    if(token.getType() != TokenType.END_LINE) {
                        if(token.getType() == TokenType.OPERATOR) {
                            if(((OperatorToken) token).operator.equals("=")) {
                                mathCompiler.reset();
                                convertMathToBytecode(type, bytecode, data, mathCompiler.compile(data, bytecode, tokens));
                            }else {
                                throw new RuntimeException("Cannot handle this token: " + token);
                            }
                        }else {
                            throw new RuntimeException("Cannot handle this token: " + token);
                        }
                    }
                } catch (UnknownTypeException unkType) {
                    String name = ((WordToken) token).word;
                    LocalVariable var = bytecode.getLocalVariable(name);
                    if(var != null) {
                        // TODO: Handle maybe an operation?
                        throw new RuntimeException("Cannot handle this token: " + token);
                    }else {
                        throw new RuntimeException("Unknown variable: " + name);
                    }
                }
            }else {
                //TODO: Not sure what this could be, maybe ++i or --i
                throw new RuntimeException("Cannot handle this token: " + token);
            }
        }
        return bytecode;
    }

    public static void convertMathToBytecode(Type type, Bytecode bytecode, CompiledData data, MathOperation op) {
        if(!type.isNumberType()) {
            throw new RuntimeException("Type is not number type!");
        }
        int size = type.type.memorySize;
        boolean unsigned = type.type.isUnsignedNumber();
        BigInteger biggestNumber;
        BigInteger smallestNumber = BigInteger.ZERO;
        if(unsigned) {
            biggestNumber = BigDecimal.valueOf(Math.pow(2, size * 8) - 1).toBigInteger();
        }else {
            biggestNumber = BigDecimal.valueOf(Math.pow(2, size * 8 - 1) - 1).toBigInteger();
            smallestNumber = BigDecimal.valueOf(-Math.pow(2, size * 8 - 1)).toBigInteger();
        }

        if(op instanceof MathBrackets) {
            convertMathToBytecode(type, bytecode, data, op.left);
        }else if(op instanceof MathCall) {
            throw new RuntimeException("Converting calls to instructions is not implemented yet");
        }else if(op instanceof MathField) {
            throw new RuntimeException("Converting fields to instructions is not implemented yet");
        }else if(op instanceof MathNumber) {
            BigInteger num = ((NumberToken) ((MathNumber) op).number).number;
            if(numberInBounds(smallestNumber, biggestNumber, num)) {
                if(size == 1) bytecode.pushInstruction(BytecodeInstructions.PUSHI8, num.byteValueExact());
                else if(size == 2) bytecode.pushInstruction(BytecodeInstructions.PUSHI16, num.shortValueExact());
                else if(size == 4) bytecode.pushInstruction(BytecodeInstructions.PUSHI32, num.intValueExact());
                else if(size == 8) bytecode.pushInstruction(BytecodeInstructions.PUSHI64, num.longValueExact());
                else throw new RuntimeException("Size " + size + " is not supported");
            }else {
                throw new RuntimeException("Number " + num + " is not in range of the type (" + smallestNumber + " to " + biggestNumber + ")");
            }
        }else {
            convertMathToBytecode(type, bytecode, data, op.left);
            convertMathToBytecode(type, bytecode, data, op.right);
            if(op instanceof MathAdd) {
                if(size == 1) bytecode.pushInstruction(BytecodeInstructions.ADD8);
                else if(size == 2) bytecode.pushInstruction(BytecodeInstructions.ADD16);
                else if(size == 4) bytecode.pushInstruction(BytecodeInstructions.ADD32);
                else if(size == 8) bytecode.pushInstruction(BytecodeInstructions.ADD64);
                else throw new RuntimeException("Size " + size + " is not supported");
            }else if(op instanceof MathSubtract) {
                if(size == 1) bytecode.pushInstruction(BytecodeInstructions.SUB8);
                else if(size == 2) bytecode.pushInstruction(BytecodeInstructions.SUB16);
                else if(size == 4) bytecode.pushInstruction(BytecodeInstructions.SUB32);
                else if(size == 8) bytecode.pushInstruction(BytecodeInstructions.SUB64);
                else throw new RuntimeException("Size " + size + " is not supported");
            }else if(op instanceof MathDivide) {
                if(size == 1 && unsigned) bytecode.pushInstruction(BytecodeInstructions.DIVU8);
                else if(size == 2 && unsigned) bytecode.pushInstruction(BytecodeInstructions.DIVU16);
                else if(size == 4 && unsigned) bytecode.pushInstruction(BytecodeInstructions.DIVU32);
                else if(size == 8 && unsigned) bytecode.pushInstruction(BytecodeInstructions.DIVU64);
                else if(size == 1 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.DIVS8);
                else if(size == 2 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.DIVS16);
                else if(size == 4 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.DIVS32);
                else if(size == 8 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.DIVS64);
                else throw new RuntimeException("Size " + size + " is not supported");
            }else if(op instanceof MathMultiply) {
                if(size == 1 && unsigned) bytecode.pushInstruction(BytecodeInstructions.MULU8);
                else if(size == 2 && unsigned) bytecode.pushInstruction(BytecodeInstructions.MULU16);
                else if(size == 4 && unsigned) bytecode.pushInstruction(BytecodeInstructions.MULU32);
                else if(size == 8 && unsigned) bytecode.pushInstruction(BytecodeInstructions.MULU64);
                else if(size == 1 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.MULS8);
                else if(size == 2 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.MULS16);
                else if(size == 4 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.MULS32);
                else if(size == 8 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.MULS64);
                else throw new RuntimeException("Size " + size + " is not supported");
            }else if(op instanceof MathMod) {
                if(size == 1 && unsigned) bytecode.pushInstruction(BytecodeInstructions.MODU8);
                else if(size == 2 && unsigned) bytecode.pushInstruction(BytecodeInstructions.MODU16);
                else if(size == 4 && unsigned) bytecode.pushInstruction(BytecodeInstructions.MODU32);
                else if(size == 8 && unsigned) bytecode.pushInstruction(BytecodeInstructions.MODU64);
                else if(size == 1 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.MODS8);
                else if(size == 2 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.MODS16);
                else if(size == 4 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.MODS32);
                else if(size == 8 && !unsigned) bytecode.pushInstruction(BytecodeInstructions.MODS64);
                else throw new RuntimeException("Size " + size + " is not supported");
            }else if(op instanceof MathAnd) {
                if(size == 1) bytecode.pushInstruction(BytecodeInstructions.AND8);
                else if(size == 2) bytecode.pushInstruction(BytecodeInstructions.AND16);
                else if(size == 4) bytecode.pushInstruction(BytecodeInstructions.AND32);
                else if(size == 8) bytecode.pushInstruction(BytecodeInstructions.AND64);
                else throw new RuntimeException("Size " + size + " is not supported");
            }else if(op instanceof MathShiftLeft) {
                if(size == 1) bytecode.pushInstruction(BytecodeInstructions.SHL8);
                else if(size == 2) bytecode.pushInstruction(BytecodeInstructions.SHL16);
                else if(size == 4) bytecode.pushInstruction(BytecodeInstructions.SHL32);
                else if(size == 8) bytecode.pushInstruction(BytecodeInstructions.SHL64);
                else throw new RuntimeException("Size " + size + " is not supported");
            }else if(op instanceof MathShiftRight) {
                if(size == 1) bytecode.pushInstruction(BytecodeInstructions.SHR8);
                else if(size == 2) bytecode.pushInstruction(BytecodeInstructions.SHR16);
                else if(size == 4) bytecode.pushInstruction(BytecodeInstructions.SHR32);
                else if(size == 8) bytecode.pushInstruction(BytecodeInstructions.SHR64);
                else throw new RuntimeException("Size " + size + " is not supported");
            }
        }
    }

    private static boolean numberInBounds(BigInteger lowest, BigInteger highest, BigInteger num) {
        return num.compareTo(lowest) >= 0 && highest.compareTo(num) > 0;
    }

    public static Function compileFunction(CompiledData data, FunctionToken functionToken, SeekIterator<Token> tokens) throws UnknownTypeException {
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
        return new Function(functionToken.name, signature, flags, parseFunctionCode(data, tokens));
    }

    public static Structure compileStructure(CompiledData data, StructureToken structureToken, SeekIterator<Token> tokens) throws UnknownTypeException {
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
            }
        }
        return flags;
    }

    public static Object compileObject(CompiledData data, ObjectToken objectToken, SeekIterator<Token> tokens) throws UnknownTypeException {
        ArrayList<ObjectField> fields = new ArrayList<>();
        ArrayList<Function> functions = new ArrayList<>();
        Token currentToken;
        while((currentToken = tokens.next()).getType() != TokenType.END_OBJECT) {
            if(currentToken.getType() == TokenType.FIELD) {
                FieldToken fieldToken = (FieldToken) currentToken;
                fields.add(new ObjectField(fieldToken.name, data.resolveType(fieldToken.type), convertFlags(fieldToken.flags)));
            }else if(currentToken.getType() == TokenType.FUNCTION) {
                functions.add(compileFunction(data, (FunctionToken) currentToken, tokens));
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
        SeekIterator<Token> tokenIter = new SeekIterator<>(tokens);
        while(tokenIter.hasNext()) {
            Token token = tokenIter.next();
            if(token.getType() == TokenType.STRUCTURE) {
                data.addStructure(compileStructure(data, (StructureToken) token, tokenIter));
            }else if(token.getType() == TokenType.OBJECT) {
                data.addObject(compileObject(data, (ObjectToken) token, tokenIter));
            }else if(token.getType() == TokenType.FUNCTION) {
                data.addFunction(compileFunction(data, (FunctionToken) token, tokenIter));
            }else if(token.getType() == TokenType.TYPEDEF) {
                TypedefToken typedefToken = (TypedefToken) token;
                data.addTypeDefinition(typedefToken.getName(), data.resolveType(typedefToken.getToType()));
            }
        }
        data.finishFutures();
        return data;
    }

}
