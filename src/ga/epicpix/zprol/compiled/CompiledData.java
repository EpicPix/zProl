package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.DataParser;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class CompiledData {

    private final ArrayList<Structure> structures = new ArrayList<>();
    private final ArrayList<Object> objects = new ArrayList<>();
    private final HashMap<String, Type> typedef = new HashMap<>();

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

    public void addFutureObjectDefinition(String name) {
        futureObjectDefinitions.add(name);
    }

    public void addFutureStructureDefinition(String name) {
        futureStructureDefinitions.add(name);
    }

    private Type finish(Type type) {
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
        }
        return type;
    }

    public void finishFutures() {
        futureObjectDefinitions.clear();
        futureStructureDefinitions.clear();

        for(Structure structure : structures) {
            for(StructureField field : structure.fields) {
                field.type = finish(field.type);
            }
        }
    }

    public Type resolveType(String type) {
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
            throw new RuntimeException("Unknown type: " + type);
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
                int flags = 0;
                for(Flag f : field.flags) {
                    flags |= f.mask;
                }
                output.writeInt(flags);
            }
        }

        output.close();
    }

    private static void writeType(Type type, DataOutputStream out) throws IOException {
        out.writeByte(type.type.id);
        if(type.type.additionalData) {
            if(type instanceof TypeFunctionSignature) {
                TypeFunctionSignature sig = (TypeFunctionSignature) type;
                writeType(sig.returnType, out);
                out.writeShort(sig.parameters.length);
                for(Type param : sig.parameters) {
                    writeType(param, out);
                }
            }else if(type instanceof TypeStructure) {
                TypeStructure sig = (TypeStructure) type;
                out.writeUTF(sig.name);
            }else if(type instanceof TypeObject) {
                TypeObject obj = (TypeObject) type;
                out.writeUTF(obj.name);
            }else {
                throw new RuntimeException("Unhandled additional data class: " + type.getClass());
            }
        }
    }

}
