package ga.epicpix.zpil.attr;

import ga.epicpix.zpil.pool.ConstantPool;
import ga.epicpix.zpil.pool.ConstantPoolEntry;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

public class ConstantValueAttribute extends Attribute {

    private Object value;

    public ConstantValueAttribute(Object value){
        super("ConstantValue");
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    protected void readData(DataInput input, ConstantPool pool) throws IOException {
        int type = input.readUnsignedByte();
        switch(type) {
            case 'n' -> value = null;
            case 's' -> value = ((ConstantPoolEntry.StringEntry) pool.entries.get(input.readInt())).getString();
            case 'b' -> value = input.readBoolean();
            case 'B' -> value = input.readByte();
            case 'S' -> value = input.readShort();
            case 'I' -> value = input.readInt();
            case 'L' -> value = input.readLong();
            default -> throw new IllegalStateException("Unknown type of constant value '" + type + "'");
        }
    }

    public void prepareConstantPool(ConstantPool pool) {
        super.prepareConstantPool(pool);
        if(value instanceof String str) {
            pool.getOrCreateStringIndex(str);
        }
    }

    protected void writeData(DataOutputStream out, ConstantPool pool) throws IOException {
        if(value == null) {
            out.writeByte('n');
        }else if(value instanceof String str) {
            out.writeByte('s');
            out.writeInt(pool.getStringIndex(str));
        }else if(value instanceof Boolean v) {
            out.writeByte('b');
            out.writeBoolean(v);
        }else if(value instanceof Byte v) {
            out.writeByte('B');
            out.writeByte(v);
        }else if(value instanceof Short v) {
            out.writeByte('S');
            out.writeShort(v);
        }else if(value instanceof Integer v) {
            out.writeByte('I');
            out.writeInt(v);
        }else if(value instanceof Long v) {
            out.writeByte('L');
            out.writeLong(v);
        }else {
            throw new IllegalStateException("Unknown type of constant value '" + value.getClass().getSimpleName() + "'");
        }
    }

}
