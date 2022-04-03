package ga.epicpix.zprol.compiled.bytecode.impl;

import ga.epicpix.zprol.compiled.bytecode.BytecodeValueType;
import ga.epicpix.zprol.compiled.bytecode.IBytecodeInstruction;
import ga.epicpix.zprol.compiled.bytecode.impl.Bytecode.BytecodeInstructionData;

import java.io.DataInput;
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

    public byte[] write() {
        int length = 0;
        BytecodeValueType[] values = data.values();
        for(int i = 0; i<values.length; i++) length += values[i].getSize();
        byte[] bytes = new byte[length];
        int index = 0;
        for(int i = 0; i<values.length; i++) {
            BytecodeValueType type = values[i];
            switch (type.getSize()) {
                case 1 -> {
                    byte b;
                    if(args[i] instanceof Number v) b = v.byteValue();
                    else throw new IllegalArgumentException(args[i].toString().getClass().getName());
                    bytes[index++] = b;
                }
                case 2 -> {
                    short s;
                    if(args[i] instanceof Number v) s = v.shortValue();
                    else throw new IllegalArgumentException(args[i].toString().getClass().getName());
                    bytes[index++] = (byte) ((s >> 8) & 0xff);
                    bytes[index++] = (byte) (s & 0xff);
                }
                case 4 -> {
                    int n;
                    if(args[i] instanceof Number v) n = v.intValue();
                    else throw new IllegalArgumentException(args[i].toString().getClass().getName());
                    bytes[index++] = (byte) ((n >> 24) & 0xff);
                    bytes[index++] = (byte) ((n >> 16) & 0xff);
                    bytes[index++] = (byte) ((n >> 8) & 0xff);
                    bytes[index++] = (byte) (n & 0xff);
                }
                case 8 -> {
                    long l;
                    if(args[i] instanceof Number v) l = v.longValue();
                    else throw new IllegalArgumentException(args[i].toString().getClass().getName());
                    bytes[index++] = (byte) ((l >> 56) & 0xff);
                    bytes[index++] = (byte) ((l >> 48) & 0xff);
                    bytes[index++] = (byte) ((l >> 40) & 0xff);
                    bytes[index++] = (byte) ((l >> 32) & 0xff);
                    bytes[index++] = (byte) ((l >> 24) & 0xff);
                    bytes[index++] = (byte) ((l >> 16) & 0xff);
                    bytes[index++] = (byte) ((l >> 8) & 0xff);
                    bytes[index++] = (byte) (l & 0xff);
                }
            }
        }
        return bytes;
    }
}
