package ga.epicpix.zprol.compiled.bytecode.impl;

import ga.epicpix.zprol.compiled.bytecode.BytecodeValueType;
import ga.epicpix.zprol.compiled.bytecode.IBytecodeInstruction;
import ga.epicpix.zprol.compiled.bytecode.impl.Bytecode.BytecodeInstructionData;

record BytecodeInstruction(BytecodeInstructionData data, Object[] args) implements IBytecodeInstruction {

    public int getId() {
        return data.id();
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
                    if(args[i] instanceof Byte) b = (Byte) args[i];
                    else if(args[i] instanceof Short) b = ((Short) args[i]).byteValue();
                    else if(args[i] instanceof Integer) b = ((Integer) args[i]).byteValue();
                    else throw new IllegalArgumentException(args[i].toString().getClass().getName());
                    bytes[index++] = b;
                }
                case 2 -> {
                    short s;
                    if(args[i] instanceof Byte) s = ((Byte) args[i]).shortValue();
                    else if(args[i] instanceof Short) s = (Short) args[i];
                    else if(args[i] instanceof Integer) s = ((Integer) args[i]).shortValue();
                    else throw new IllegalArgumentException(args[i].toString().getClass().getName());
                    bytes[index++] = (byte) (s & 0xff);
                    bytes[index++] = (byte) ((s >> 8) & 0xff);
                }
                case 4 -> {
                    int n;
                    if(args[i] instanceof Byte) n = ((Byte) args[i]).intValue();
                    else if(args[i] instanceof Short) n = ((Short) args[i]).intValue();
                    else if(args[i] instanceof Integer) n = (Integer) args[i];
                    else throw new IllegalArgumentException(args[i].toString().getClass().getName());
                    bytes[index++] = (byte) (n & 0xff);
                    bytes[index++] = (byte) ((n >> 8) & 0xff);
                    bytes[index++] = (byte) ((n >> 16) & 0xff);
                    bytes[index++] = (byte) ((n >> 24) & 0xff);
                }
            }
        }
        return bytes;
    }
}
