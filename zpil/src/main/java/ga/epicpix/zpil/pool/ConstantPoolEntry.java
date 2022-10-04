package ga.epicpix.zpil.pool;

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
        return switch(t) {
            case 1 -> StringEntry.read(in);
            default -> throw new RuntimeException("Unknown tag: " + t);
        };
    }

    public static class StringEntry extends ConstantPoolEntry {

        private final String str;

        public StringEntry(String str) {
            super((byte) 1);
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

        public String toString() {
            return "StringEntry string=\"" + str.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
        }
    }

}
