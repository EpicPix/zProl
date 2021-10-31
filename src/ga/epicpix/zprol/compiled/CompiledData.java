package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.DataParser;
import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.exceptions.FunctionNotDefinedException;
import ga.epicpix.zprol.exceptions.UnknownTypeException;
import ga.epicpix.zprol.tokens.Token;
import ga.epicpix.zprol.tokens.TokenType;
import ga.epicpix.zprol.tokens.WordToken;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class CompiledData {

    private final ArrayList<Structure> structures = new ArrayList<>();
    private final ArrayList<Object> objects = new ArrayList<>();
    private final ArrayList<Function> functions = new ArrayList<>();
    private final HashMap<String, Type> typedef = new HashMap<>();

    public ArrayList<Structure> getStructures() {
        return new ArrayList<>(structures);
    }

    public ArrayList<Object> getObjects() {
        return new ArrayList<>(objects);
    }

    public ArrayList<Function> getFunctions() {
        return new ArrayList<>(functions);
    }

    public Function getFunction(String name, TypeFunctionSignature sig) {
        for(Function func : functions) {
            if(func.name.equals(name)) {
                if(sig.returnType == null || func.signature.returnType.type == sig.returnType.type) {
                    if(func.signature.parameters.length == sig.parameters.length) {
                        boolean success = true;
                        for(int i = 0; i<func.signature.parameters.length; i++) {
                            if((func.signature.parameters[i].type.type != sig.parameters[i].type) && !(func.signature.parameters[i].type.type.isNumberType() && sig.parameters[i].type == Types.NUMBER)) {
                                success = false;
                                break;
                            }
                        }
                        if(success) {
                            return func;
                        }
                    }
                }
            }
        }
        throw new FunctionNotDefinedException(name);
    }

    private final ArrayList<String> futureObjectDefinitions = new ArrayList<>();
    private final ArrayList<String> futureStructureDefinitions = new ArrayList<>();

    public void addTypeDefinition(String typeName, Type type) {
        if(typedef.get(typeName) == null) {
            typedef.put(typeName, type);
        }else {
            throw new RuntimeException("Type definition for " + typeName + " is already defined");
        }
    }

    public void addStructure(Structure structure) {
        structures.add(structure);
    }

    public void addObject(Object object) {
        objects.add(object);
    }

    public void addFunction(Function function) {
        functions.add(function);
    }

    public void addFutureObjectDefinition(String name) {
        futureObjectDefinitions.add(name);
    }

    public void addFutureStructureDefinition(String name) {
        futureStructureDefinitions.add(name);
    }

    private Type finish(Type type) throws UnknownTypeException {
        if(type instanceof TypeFutureObject) {
            type = resolveType(((TypeFutureObject) type).type);
        }else if(type instanceof TypeFutureStructure) {
            type = resolveType(((TypeFutureStructure) type).type);
        }else if(type instanceof TypeFunctionSignature) {
            TypeFunctionSignature sig = (TypeFunctionSignature) type;
            sig.returnType = finish(sig.returnType);
            for(int i = 0; i<sig.parameters.length; i++) {
                sig.parameters[i] = finish(sig.parameters[i]);
            }
        }else if(type instanceof TypeFunctionSignatureNamed) {
            TypeFunctionSignatureNamed sig = (TypeFunctionSignatureNamed) type;
            sig.returnType = finish(sig.returnType);
            for(int i = 0; i<sig.parameters.length; i++) {
                sig.parameters[i] = (TypeNamed) finish(sig.parameters[i]);
            }
        }else if(type instanceof TypeNamed) {
            TypeNamed named = (TypeNamed) type;
            named.type = finish(named.type);
        }
        return type;
    }

    public void finishFutures() throws UnknownTypeException {
        futureObjectDefinitions.clear();
        futureStructureDefinitions.clear();

        for(Structure structure : structures) {
            for(StructureField field : structure.fields) {
                field.type = finish(field.type);
            }
        }

        for(Object object : objects) {
            for(ObjectField field : object.fields) {
                field.type = finish(field.type);
            }

            for(Function func : object.functions) {
                func.signature = (TypeFunctionSignatureNamed) finish(func.signature);
            }
        }

        for(Function func : functions) {
            func.signature = (TypeFunctionSignatureNamed) finish(func.signature);
        }
    }

    public Type resolveType(SeekIterator<Token> iter) throws UnknownTypeException {
        String type = ((WordToken) iter.next()).word;
        if(type == null) return new Type(Types.NONE);
        if(iter.seek().getType() == TokenType.OPEN) {
            iter.next();
            Type ret = resolveType(type);
            ArrayList<Type> parameters = new ArrayList<>();
            while(true) {
                if(iter.seek().getType() == TokenType.CLOSE) {
                    iter.next();
                    break;
                }
                Type param = resolveType(iter);
                parameters.add(param);

                if(iter.seek().getType() == TokenType.COMMA) {
                    iter.next();
                }
            }
            return new TypeFunctionSignature(ret, parameters.toArray(new Type[0]));
        }
        if(type.equals("int8")) return new Type(Types.INT8);
        else if(type.equals("int16")) return new Type(Types.INT16);
        else if(type.equals("int32")) return new Type(Types.INT32);
        else if(type.equals("int64")) return new Type(Types.INT64);
        else if(type.equals("uint8")) return new Type(Types.UINT8);
        else if(type.equals("uint16")) return new Type(Types.UINT16);
        else if(type.equals("uint32")) return new Type(Types.UINT32);
        else if(type.equals("uint64")) return new Type(Types.UINT64);
        else if(type.equals("pointer")) return new Type(Types.POINTER);
        else if(type.equals("void")) return new Type(Types.VOID);
        Type t = typedef.get(type);
        if(t != null) return t;

        for(Structure struct : structures) {
            if(struct.name.equals(type)) {
                return new TypeStructure(struct.name);
            }
        }

        for(Object obj : objects) {
            if(obj.name.equals(type)) {
                return new TypeObject(obj.name);
            }
        }

        if(futureObjectDefinitions.contains(type)) {
            return new TypeFutureObject(type);
        }
        if(futureStructureDefinitions.contains(type)) {
            return new TypeFutureStructure(type);
        }

        throw new UnknownTypeException("Unknown type: " + type);
    }

    public Type resolveType(String type) throws UnknownTypeException {
        if(type == null) return new Type(Types.NONE);
        if(type.equals("int8")) return new Type(Types.INT8);
        else if(type.equals("int16")) return new Type(Types.INT16);
        else if(type.equals("int32")) return new Type(Types.INT32);
        else if(type.equals("int64")) return new Type(Types.INT64);
        else if(type.equals("uint8")) return new Type(Types.UINT8);
        else if(type.equals("uint16")) return new Type(Types.UINT16);
        else if(type.equals("uint32")) return new Type(Types.UINT32);
        else if(type.equals("uint64")) return new Type(Types.UINT64);
        else if(type.equals("pointer")) return new Type(Types.POINTER);
        else if(type.equals("void")) return new Type(Types.VOID);
        Type t = typedef.get(type);
        if(t != null) return t;

        for(Structure struct : structures) {
            if(struct.name.equals(type)) {
                return new TypeStructure(struct.name);
            }
        }

        for(Object obj : objects) {
            if(obj.name.equals(type)) {
                return new TypeObject(obj.name);
            }
        }

        if(futureObjectDefinitions.contains(type)) {
            return new TypeFutureObject(type);
        }
        if(futureStructureDefinitions.contains(type)) {
            return new TypeFutureStructure(type);
        }

        DataParser parser = new DataParser(type);

        String pre = parser.nextWord();
        if(parser.seekWord() == null || !parser.nextWord().equals("(")) {
            throw new UnknownTypeException("Unknown type: " + type);
        }
        Type ret = resolveType(pre);
        ArrayList<Type> parameters = new ArrayList<>();
        while(true) {
            if(parser.seekWord().equals(")")) {
                parser.nextWord();
                break;
            }
            Type param = resolveType(parser.nextType());
            parameters.add(param);

            if(parser.seekWord().equals(",")) {
                parser.nextWord();
            }
        }
        return new TypeFunctionSignature(ret, parameters.toArray(new Type[0]));
    }

    public void save(File file) throws IOException {
        DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        output.writeBytes("zPRL");

        output.writeShort(structures.size());
        for(Structure struct : structures) {
            output.writeUTF(struct.name);
            output.writeShort(struct.fields.size());
            for(StructureField field : struct.fields) {
                output.writeUTF(field.name);
                writeType(field.type, output);
            }
        }

        output.writeShort(objects.size());
        for(Object obj : objects) {
            output.writeUTF(obj.name);
            writeType(obj.ext, output);
            output.writeShort(obj.fields.size());
            for(ObjectField field : obj.fields) {
                output.writeUTF(field.name);
                writeType(field.type, output);
                writeFlags(field.flags, output);
            }
            output.writeShort(obj.functions.size());
            for(Function func : obj.functions) {
                writeFunction(func, output);
            }
        }

        output.writeShort(functions.size());
        for(Function func : functions) {
            writeFunction(func, output);
        }

        output.close();
    }

    private static void writeFlags(ArrayList<Flag> flags, DataOutputStream out) throws IOException {
        int flagsOut = 0;
        for(Flag f : flags) {
            flagsOut |= f.mask;
        }
        out.writeInt(flagsOut);
    }

    private static void writeFunction(Function func, DataOutputStream out) throws IOException {
        out.writeUTF(func.name);
        writeFlags(func.flags, out);
        writeFunctionSignatureTypeNamed(func.signature, out);
        if(!func.flags.contains(Flag.NO_IMPLEMENTATION)) {
            func.code.write(out);
        }
    }

    private static void writeFunctionSignatureTypeNamed(TypeFunctionSignatureNamed sig, DataOutputStream out) throws IOException {
        writeType(sig.returnType, out);
        out.writeShort(sig.parameters.length);
        for(Type param : sig.parameters) {
            writeType(param, out);
        }
    }

    private static void writeFunctionSignatureType(TypeFunctionSignature sig, DataOutputStream out) throws IOException {
        writeType(sig.returnType, out);
        out.writeShort(sig.parameters.length);
        for(Type param : sig.parameters) {
            writeType(param, out);
        }
    }

    private static void writeType(Type type, DataOutputStream out) throws IOException {
        out.writeByte(type.type.id);
        if(type.type.additionalData) {
            if(type instanceof TypeFunctionSignature) {
                writeFunctionSignatureType((TypeFunctionSignature) type, out);
            }else if(type instanceof TypeStructure) {
                TypeStructure sig = (TypeStructure) type;
                out.writeUTF(sig.name);
            }else if(type instanceof TypeObject) {
                TypeObject obj = (TypeObject) type;
                out.writeUTF(obj.name);
            }else if(type instanceof TypeNamed) {
                TypeNamed named = (TypeNamed) type;
                out.writeUTF(named.name);
                writeType(named.type, out);
            }else {
                throw new RuntimeException("Unhandled additional data class: " + type.getClass());
            }
        }
    }

}
