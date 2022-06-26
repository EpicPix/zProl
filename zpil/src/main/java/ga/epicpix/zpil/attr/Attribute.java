package ga.epicpix.zpil.attr;

import ga.epicpix.zpil.pool.ConstantPool;
import ga.epicpix.zpil.pool.ConstantPoolEntry;

import java.io.*;

public abstract class Attribute {

    private final String name;

    public Attribute(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Attribute read(DataInput input, ConstantPool pool) throws IOException {
        String name = ((ConstantPoolEntry.StringEntry) pool.entries.get(input.readInt())).getString();
        int length = input.readInt();
        byte[] data = new byte[length];
        for(int i = 0; i<length; i++) data[i] = input.readByte();
        Attribute inst = switch(name) {
            case "ConstantValue" -> new ConstantValueAttribute(null);
            default -> null;
        };
        if(inst == null) return null;
        inst.readData(new DataInputStream(new ByteArrayInputStream(data)), pool);
        return inst;
    }

    public void prepareConstantPool(ConstantPool pool) {
        pool.getOrCreateStringIndex(name);
    }

    public final byte[] write(ConstantPool pool) throws IOException {
        var arr = new ByteArrayOutputStream();
        var out = new DataOutputStream(arr);
        out.writeInt(pool.getStringIndex(name));
        var data = new ByteArrayOutputStream();
        writeData(new DataOutputStream(data), pool);
        out.writeInt(data.size());
        out.write(data.toByteArray());
        return arr.toByteArray();
    }

    protected abstract void readData(DataInput input, ConstantPool pool) throws IOException;
    protected abstract void writeData(DataOutputStream out, ConstantPool pool) throws IOException;

}
