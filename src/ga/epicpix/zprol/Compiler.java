package ga.epicpix.zprol;

import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.compiled.Flag;
import ga.epicpix.zprol.compiled.Function;
import ga.epicpix.zprol.compiled.bytecode.Bytecode;
import ga.epicpix.zprol.compiled.LocalVariable;
import ga.epicpix.zprol.compiled.bytecode.BytecodeInstructions;
import ga.epicpix.zprol.compiled.math.MathCompiler;
import ga.epicpix.zprol.compiled.Object;
import ga.epicpix.zprol.compiled.ObjectField;
import ga.epicpix.zprol.compiled.Structure;
import ga.epicpix.zprol.compiled.StructureField;
import ga.epicpix.zprol.compiled.Type;
import ga.epicpix.zprol.compiled.TypeFunctionSignatureNamed;
import ga.epicpix.zprol.compiled.TypeNamed;
import ga.epicpix.zprol.compiled.math.MathOperation;
import ga.epicpix.zprol.exceptions.UnknownTypeException;
import ga.epicpix.zprol.tokens.FieldToken;
import ga.epicpix.zprol.tokens.FunctionToken;
import ga.epicpix.zprol.tokens.ObjectToken;
import ga.epicpix.zprol.tokens.OperatorToken;
import ga.epicpix.zprol.tokens.StructureToken;
import ga.epicpix.zprol.tokens.Token;
import ga.epicpix.zprol.tokens.TokenType;
import ga.epicpix.zprol.tokens.TypedefToken;
import ga.epicpix.zprol.tokens.WordToken;
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
                                convertMathToBytecode(bytecode, data, mathCompiler.compile(data, bytecode, tokens));
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

    public static void convertMathToBytecode(Bytecode bytecode, CompiledData data, MathOperation op) {

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
