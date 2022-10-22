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
        String name = ((ConstantPoolEntry.StringEntry) pool.entries.get(input.readInt() - 1)).getString();
        int length = input.readInt();
        byte[] data = new byte[length];
        for(int i = 0; i<length; i++) data[i] = input.readByte();
        Attribute inst;
        if("ConstantValue".equals(name)) {
            inst = new ConstantValueAttribute(null);
        } else {
            inst = null;
        }
        if(inst == null) return null;
        inst.readData(new DataInputStream(new ByteArrayInputStream(data)), pool);
        return inst;
    }

    public void prepareConstantPool(ConstantPool pool) {
        pool.getOrCreateStringIndex(name);
    }

    public final byte[] write(ConstantPool pool) throws IOException {
        ByteArrayOutputStream arr = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(arr);
        out.writeInt(pool.getStringIndex(name));
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        writeData(new DataOutputStream(data), pool);
        out.writeInt(data.size());
        out.write(data.toByteArray());
        return arr.toByteArray();
    }

    protected abstract void readData(DataInput input, ConstantPool pool) throws IOException;
    protected abstract void writeData(DataOutputStream out, ConstantPool pool) throws IOException;

}
