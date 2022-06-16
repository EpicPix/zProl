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
            case 1 -> FunctionEntry.read(in);
            case 2 -> StringEntry.read(in);
            case 3 -> ClassEntry.read(in);
            case 4 -> MethodEntry.read(in);
            case 5 -> FieldEntry.read(in);
            default -> throw new RuntimeException("Unknown tag: " + t);
        };
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

        public String toString() {
            String mod = Integer.toHexString(modifiers);
            mod = "0".repeat(8 - mod.length()) + mod;
            return "FunctionEntry namespace=#" + namespace + " name=#" + name + " signature=#" + sig + " modifiers=0x" + mod;
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

        public String toString() {
            return "StringEntry string=\"" + str.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
        }
    }

    public static class ClassEntry extends ConstantPoolEntry {

        private final int namespace;
        private final int name;

        public ClassEntry(int namespace, int name) {
            super((byte) 3);
            this.namespace = namespace;
            this.name = name;
        }

        public int getNamespace() {
            return namespace;
        }

        public int getName() {
            return name;
        }

        public void write(DataOutputStream out) throws IOException {
            super.write(out);
            out.writeInt(namespace);
            out.writeInt(name);
        }

        public static ClassEntry read(DataInputStream in) throws IOException {
            return new ClassEntry(in.readInt(), in.readInt());
        }

        public String toString() {
            return "ClassEntry namespace=#" + namespace + " name=#" + name;
        }
    }

    public static class MethodEntry extends ConstantPoolEntry {

        private final int namespace;
        private final int className;
        private final int name;
        private final int sig;
        private final int modifiers;

        public MethodEntry(int namespace, int className, int name, int sig, int modifiers) {
            super((byte) 4);
            this.namespace = namespace;
            this.className = className;
            this.name = name;
            this.sig = sig;
            this.modifiers = modifiers;
        }

        public int getNamespace() {
            return namespace;
        }

        public int getClassName() {
            return className;
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
            out.writeInt(className);
            out.writeInt(name);
            out.writeInt(sig);
            out.writeInt(modifiers);
        }

        public static MethodEntry read(DataInputStream in) throws IOException {
            return new MethodEntry(in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt());
        }

        public String toString() {
            String mod = Integer.toHexString(modifiers);
            mod = "0".repeat(8 - mod.length()) + mod;
            return "MethodEntry namespace=#" + namespace + " className=#" + className + " name=#" + name + " signature=#" + sig + " modifiers=0x" + mod;
        }
    }

    public static class FieldEntry extends ConstantPoolEntry {

        private final int namespace;
        private final int name;
        private final int type;

        public FieldEntry(int namespace, int name, int type) {
            super((byte) 5);
            this.namespace = namespace;
            this.name = name;
            this.type = type;
        }

        public int getNamespace() {
            return namespace;
        }

        public int getName() {
            return name;
        }

        public int getType() {
            return type;
        }

        public void write(DataOutputStream out) throws IOException {
            super.write(out);
            out.writeInt(namespace);
            out.writeInt(name);
            out.writeInt(type);
        }

        public static FieldEntry read(DataInputStream in) throws IOException {
            return new FieldEntry(in.readInt(), in.readInt(), in.readInt());
        }

        public String toString() {
            return "FieldEntry namespace=#" + namespace + " name=#" + name + " type=#" + type;
        }
    }

}
