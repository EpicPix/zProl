package ga.epicpix.zprol.bytecode.impl;

import ga.epicpix.zprol.compiled.ConstantPool;
import ga.epicpix.zprol.compiled.Function;
import ga.epicpix.zprol.bytecode.BytecodeValueType;
import ga.epicpix.zprol.bytecode.IBytecodeInstruction;
import ga.epicpix.zprol.bytecode.impl.Bytecode.BytecodeInstructionData;
import ga.epicpix.zprol.exceptions.InvalidOperationException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

record BytecodeInstruction(BytecodeInstructionData data, Object[] args) implements IBytecodeInstruction {

    public String toString() {
        return getName() + (args.length != 0 ? " " + Arrays.toString(args).replace("\n", "\\n") : "");
    }

    public int getId() {
        return data.id();
    }

    public String getName() {
        return data.name();
    }

    public Object[] getData() {
        return args;
    }

    public void prepareConstantPool(ConstantPool pool) {
        var values = data.values();
        for(int i = 0; i<values.length; i++) {
            var type = values[i];
            var val = args[i];
            if(type == BytecodeValueType.STRING) {
                if(val instanceof String v) pool.getOrCreateStringIndex(v);
                else throw new IllegalArgumentException(val.toString().getClass().getName());
            }else if(type == BytecodeValueType.FUNCTION) {
                if(val instanceof Function v) pool.getOrCreateFunctionIndex(v);
                else throw new IllegalArgumentException(val.toString().getClass().getName());
            }
        }
    }

    public byte[] write(ConstantPool pool) throws IOException {
        var bytes = new ByteArrayOutputStream();
        var out = new DataOutputStream(bytes);
        out.writeByte(getId());
        var values = data.values();
        for(int i = 0; i<values.length; i++) {
            var type = values[i];
            var val = args[i];
            if(type == BytecodeValueType.STRING) {
                if(val instanceof String v) val = pool.getStringIndex(v);
                else throw new IllegalArgumentException(val.toString().getClass().getName());
            }else if(type == BytecodeValueType.FUNCTION) {
                if(val instanceof Function v) val = pool.getFunctionIndex(v);
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
