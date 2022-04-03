package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.exceptions.InvalidDataException;

import java.io.*;

public class GeneratedData {

    public GeneratedData addCompiled(CompiledData data) {

        return this;
    }

    public GeneratedData addCompiled(CompiledData... data) {
        for(CompiledData d : data) {
            addCompiled(d);
        }
        return this;
    }

    public static byte[] save(GeneratedData data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeBytes("zPrl");

        out.close();
        return bytes.toByteArray();
    }

    public static GeneratedData load(byte[] b) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(b));
        GeneratedData data = new GeneratedData();

        if(!new String(in.readNBytes(4)).equals("zPrl")) {
            throw new InvalidDataException("invalid magic");
        }

        in.close();
        return data;
    }

}
