package ga.epicpix.zprol.compiled;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class CompiledData {

    private final ArrayList<Structure> structures = new ArrayList<>();

    public Type resolveType(String type) {
//        throw new UnsupportedOperationException("Cannot resolve types yet: " + type);
        return null;
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
