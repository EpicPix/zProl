package ga.epicpix.zprol.compiled;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class CompiledData {

    private final ArrayList<Structure> structures = new ArrayList<>();
    private final HashMap<String, Type> typedef = new HashMap<>();

    public void addTypeDefinition(String typeName, Type type) {
        if(typedef.get(typeName) == null) {
            typedef.put(typeName, type);
        }else {
            throw new RuntimeException("Type definition for " + typeName + " is already defined");
        }
    }

    public Type resolveType(String type) {
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
        throw new RuntimeException("Unknown type: " + type);
    }

    public void addStructure(Structure structure) {
        structures.add(structure);
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
            }
        }
    }

}
