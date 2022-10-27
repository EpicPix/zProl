package ga.epicpix.zpil.attr;

import ga.epicpix.zpil.StringTable;

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

    protected void readData(DataInput input, StringTable pool) throws IOException {
        int type = input.readUnsignedByte();
        switch(type) {
            case 'n':
                value = null;
                break;
            case 's':
                value = pool.entries.get(input.readInt() - 1);
                break;
            case 'b':
                value = input.readBoolean();
                break;
            case 'B':
                value = input.readByte();
                break;
            case 'S':
                value = input.readShort();
                break;
            case 'I':
                value = input.readInt();
                break;
            case 'L':
                value = input.readLong();
                break;
            default:
                throw new IllegalStateException("Unknown type of constant value '" + type + "'");
        }
    }

    public void prepareConstantPool(StringTable pool) {
        super.prepareConstantPool(pool);
        if(value instanceof String) {
            pool.getOrCreateStringIndex((String) value);
        }
    }

    protected void writeData(DataOutputStream out, StringTable pool) throws IOException {
        if(value == null) {
            out.writeByte('n');
        }else if(value instanceof String) {
            out.writeByte('s');
            out.writeInt(pool.getStringIndex((String) value));
        }else if(value instanceof Boolean) {
            out.writeByte('b');
            out.writeBoolean((Boolean) value);
        }else if(value instanceof Byte) {
            out.writeByte('B');
            out.writeByte((Byte) value);
        }else if(value instanceof Short) {
            out.writeByte('S');
            out.writeShort((Short) value);
        }else if(value instanceof Integer) {
            out.writeByte('I');
            out.writeInt((Integer) value);
        }else if(value instanceof Long) {
            out.writeByte('L');
            out.writeLong((Long) value);
        }else {
            throw new IllegalStateException("Unknown type of constant value '" + value.getClass().getSimpleName() + "'");
        }
    }

}
