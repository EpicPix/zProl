package ga.epicpix.zprol.compiled;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ConstantPoolEntry {

    private final byte tag;

    public ConstantPoolEntry(byte tag) {
        this.tag = tag;
    }

    public byte getTag() {
        return tag;
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeByte(tag);
    }

    public static ConstantPoolEntry read(DataInputStream in) throws IOException {
        byte t = in.readByte();
        if(t == 1) {
            return FunctionEntry.read(in);
        }
        throw new RuntimeException("Unknown tag: " + t);
    }

    public static class FunctionEntry extends ConstantPoolEntry {

        private final String name;
        private final TypeFunctionSignature sig;

        public FunctionEntry(Function func) {
            super((byte) 1);
            name = func.name;
            sig = new TypeFunctionSignature(func.signature);
        }

        public FunctionEntry(String name, TypeFunctionSignature sig) {
            super((byte) 1);
            this.name = name;
            this.sig = sig;
        }

        public String getName() {
            return name;
        }

        public TypeFunctionSignature getSignature() {
            return sig;
        }

        public void write(DataOutputStream out) throws IOException {
            super.write(out);
            out.writeUTF(name);
            CompiledData.writeFunctionSignatureType(sig, out);
        }

        public static FunctionEntry read(DataInputStream in) throws IOException {
            return new FunctionEntry(in.readUTF(), CompiledData.readFunctionSignature(in));
        }
    }

}
