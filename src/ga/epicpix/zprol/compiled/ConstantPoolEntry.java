package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.exceptions.NotImplementedException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ConstantPoolEntry {

    private final byte tag;

    public ConstantPoolEntry(byte tag) {
        this.tag = tag;
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeByte(tag);
    }

    public static ConstantPoolEntry read(DataInputStream in) throws IOException {
        byte t = in.readByte();
        if(t == 1) {
            return FunctionEntry.read(in);
        }else if(t == 2) {
            return StringEntry.read(in);
        }
        throw new RuntimeException("Unknown tag: " + t);
    }

    public static class FunctionEntry extends ConstantPoolEntry {

        private final String namespace;
        private final String name;
        private final FunctionSignature sig;

        public FunctionEntry(String namespace, Function func) {
            super((byte) 1);
            this.namespace = namespace;
            name = func.name();
            sig = func.signature();
        }

        public FunctionEntry(String namespace, String name, FunctionSignature sig) {
            super((byte) 1);
            this.namespace = namespace;
            this.name = name;
            this.sig = sig;
        }

        public String getNamespace() {
            return namespace;
        }

        public String getName() {
            return name;
        }

        public FunctionSignature getSignature() {
            return sig;
        }

        public void write(DataOutputStream out) throws IOException {
            super.write(out);
            throw new NotImplementedException("Writing function entries is not implemented yet");
        }

        public static FunctionEntry read(DataInputStream in) throws IOException {
            throw new NotImplementedException("Reading function entries is not implemented yet");
        }
    }

    public static class StringEntry extends ConstantPoolEntry {

        private final String str;

        public StringEntry(String str) {
            super((byte) 2);
            this.str = str;
        }

        public String getString() {
            return str;
        }

        public void write(DataOutputStream out) throws IOException {
            super.write(out);
            out.writeUTF(str);
        }

        public static StringEntry read(DataInputStream in) throws IOException {
            return new StringEntry(in.readUTF());
        }
    }

}
