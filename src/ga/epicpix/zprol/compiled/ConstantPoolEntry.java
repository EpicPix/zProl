package ga.epicpix.zprol.compiled;

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

        private final int namespace;
        private final int name;
        private final int sig;
        private final int modifiers;

        public FunctionEntry(int namespace, int name, int sig, int modifiers) {
            super((byte) 1);
            this.namespace = namespace;
            this.name = name;
            this.sig = sig;
            this.modifiers = modifiers;
        }

        public int getNamespace() {
            return namespace;
        }

        public int getName() {
            return name;
        }

        public int getSignature() {
            return sig;
        }

        public int getModifiers() {
            return modifiers;
        }

        public void write(DataOutputStream out) throws IOException {
            super.write(out);
            out.writeInt(namespace);
            out.writeInt(name);
            out.writeInt(sig);
            out.writeInt(modifiers);
        }

        public static FunctionEntry read(DataInputStream in) throws IOException {
            return new FunctionEntry(in.readInt(), in.readInt(), in.readInt(), in.readInt());
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
