package ga.epicpix.zprol.compiled.bytecode.impl;

import ga.epicpix.zprol.compiled.ConstantPool;
import ga.epicpix.zprol.compiled.bytecode.BytecodeValueType;
import ga.epicpix.zprol.compiled.bytecode.IBytecodeInstruction;
import ga.epicpix.zprol.compiled.bytecode.impl.Bytecode.BytecodeInstructionData;
import ga.epicpix.zprol.exceptions.InvalidOperationException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

record BytecodeInstruction(BytecodeInstructionData data, Object[] args) implements IBytecodeInstruction {

    public String toString() {
        return "BytecodeInstruction[id=" + getId() + ", name=" + getName() + "]";
    }

    public int getId() {
        return data.id();
    }

    public String getName() {
        return data.name();
    }

    public void prepareConstantPool(ConstantPool pool) {
        var values = data.values();
        for(int i = 0; i<values.length; i++) {
            var type = values[i];
            var val = args[i];
            if(type == BytecodeValueType.STRING) {
                if(val instanceof String v) pool.getOrCreateStringIndex(v);
                else throw new IllegalArgumentException(val.toString().getClass().getName());
            }
        }
    }

    public byte[] write(ConstantPool pool) throws IOException {
        var bytes = new ByteArrayOutputStream();
        var out = new DataOutputStream(bytes);
        var values = data.values();
        for(int i = 0; i<values.length; i++) {
            var type = values[i];
            var val = args[i];
            if(type == BytecodeValueType.STRING) {
                if(val instanceof String v) val = pool.getStringIndex(v);
                else throw new IllegalArgumentException(val.toString().getClass().getName());
            }
            if(!(val instanceof Number num)) throw new IllegalArgumentException(val.toString().getClass().getName());
            switch (type.getSize()) {
                case 1 -> out.writeByte(num.byteValue());
                case 2 -> out.writeShort(num.shortValue());
                case 4 -> out.writeInt(num.intValue());
                case 8 -> out.writeLong(num.longValue());
                default -> throw new InvalidOperationException("Invalid size of bytecode type: " + type.getSize());
            }
        }
        return bytes.toByteArray();
    }
}