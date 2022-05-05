package ga.epicpix.zprol.bytecode.impl;

import ga.epicpix.zprol.bytecode.BytecodeValueType;
import ga.epicpix.zprol.bytecode.IBytecode;
import ga.epicpix.zprol.bytecode.IBytecodeInstructionGenerator;
import ga.epicpix.zprol.bytecode.IBytecodeStorage;
import ga.epicpix.zprol.exceptions.bytecode.RedefinedInstructionException;
import ga.epicpix.zprol.exceptions.bytecode.UnknownInstructionException;

import java.util.ArrayList;

public final class Bytecode implements IBytecode {

    public static final IBytecode BYTECODE = new Bytecode();

    /* package-private */ record BytecodeInstructionData(int id, String name, BytecodeValueType[] values, IBytecodeInstructionGenerator generator) {}

    private final ArrayList<BytecodeInstructionData> data = new ArrayList<>();

    private Bytecode() {
        registerSizedInstruction(0, "return", new int[] {0, 1, 2, 4, 8});
        registerSizedInstruction(5, "store_local", new int[] {1, 2, 4, 8}, BytecodeValueType.SHORT);
        registerInstruction(9, "invoke", BytecodeValueType.FUNCTION);
        // id 10 is not used
        // id 11 is not used
        // id 12 is not used
        // id 13 is not used
        // id 14 is not used
        // id 15 is not used
        registerSizedInstruction(16, "pop", new int[] {1, 2, 4, 8});
        registerSizedInstruction(20, "load_local", new int[] {1, 2, 4, 8}, BytecodeValueType.SHORT);
        registerSizedInstruction(24, "cast" + getInstructionPrefix(1), new int[] {2, 4, 8});
        registerSizedInstruction(27, "cast" + getInstructionPrefix(2), new int[] {1, 4, 8});
        registerSizedInstruction(30, "cast" + getInstructionPrefix(4), new int[] {1, 2, 8});
        registerSizedInstruction(33, "cast" + getInstructionPrefix(8), new int[] {1, 2, 4});
        registerInstruction(36, getInstructionPrefix(1) + "push", BytecodeValueType.BYTE);
        registerInstruction(37, getInstructionPrefix(2) + "push", BytecodeValueType.SHORT);
        registerInstruction(38, getInstructionPrefix(4) + "push", BytecodeValueType.INT);
        registerInstruction(39, getInstructionPrefix(8) + "push", BytecodeValueType.LONG);
        registerSizedInstruction(40, "compare", new int[] {1, 2, 4, 8});
        registerSizedInstruction(44, "compare_not", new int[] {1, 2, 4, 8});
        registerSizedInstruction(48, "add", new int[] {1, 2, 4, 8});
        registerSizedInstruction(52, "sub", new int[] {1, 2, 4, 8});
        registerSizedInstruction(56, "mul", new int[] {1, 2, 4, 8});
        registerSizedInstruction(60, "mulu", new int[] {1, 2, 4, 8});
        registerSizedInstruction(64, "div", new int[] {1, 2, 4, 8});
        registerSizedInstruction(68, "divu", new int[] {1, 2, 4, 8});
        registerSizedInstruction(72, "mod", new int[] {1, 2, 4, 8});
        registerSizedInstruction(76, "modu", new int[] {1, 2, 4, 8});
        registerSizedInstruction(80, "and", new int[] {1, 2, 4, 8});
        registerSizedInstruction(84, "shift_left", new int[] {1, 2, 4, 8});
        registerSizedInstruction(88, "shift_right", new int[] {1, 2, 4, 8});
        registerInstruction(92, "push_string", BytecodeValueType.STRING);
        registerSizedInstruction(93, "dup", new int[] {1, 2, 4, 8});
        registerSizedInstruction(97, "or", new int[] {1, 2, 4, 8});
    }

    private void registerSizedInstruction(int id, String name, int[] sizes, BytecodeValueType... values) {
        for(int i = 0; i<sizes.length; i++) {
            registerInstruction(id + i, getInstructionPrefix(sizes[i]) + name, values);
        }
    }

    public void registerInstruction(int id, String name, BytecodeValueType... values) {
        for(BytecodeInstructionData d : data) {
            if(d.id == id) {
                throw new RedefinedInstructionException("Instruction with id '" + d.id + "' already registered with name '" + d.name + "'");
            }else if(d.name.equals(name)) {
                throw new RedefinedInstructionException("Instruction with name '" + d.name + "' already registered with id '" + d.id + "'");
            }
        }
        data.add(new BytecodeInstructionData(id, name, values, (args) -> {
            for(BytecodeInstructionData d : data) {
                if(d.id == id) {
                    if(d.values.length != args.length) throw new IllegalArgumentException("Invalid amount of arguments (" + args.length + "), expected " + d.values.length);

                    return new BytecodeInstruction(d, args);
                }
            }
            throw new UnknownInstructionException("Data about instruction not found (" + id + ")");
        }));
    }

    public String getInstructionPrefix(int size) {
        return switch(size) {
            case 0 -> "v";
            case 1 -> "b";
            case 2 -> "s";
            case 4 -> "i";
            case 8 -> "l";
            case 16 -> "h";
            default -> throw new IllegalArgumentException("Instruction prefix with size " + size + " does not exist");
        };
    }

    public IBytecodeInstructionGenerator getInstruction(int id) {
        for(BytecodeInstructionData d : data) {
            if(d.id == id) {
                return d.generator;
            }
        }
        throw new UnknownInstructionException("Instruction with id '" + id + "' not found");
    }

    public BytecodeValueType[] getInstructionValueTypesRequirements(int id) {
        for(BytecodeInstructionData d : data) {
            if(d.id == id) {
                var clone = new BytecodeValueType[d.values.length];
                System.arraycopy(d.values, 0, clone, 0, d.values.length);
                return clone;
            }
        }
        throw new UnknownInstructionException("Instruction with id '" + id + "' not found");
    }

    public IBytecodeInstructionGenerator getInstruction(String name) {
        for(BytecodeInstructionData d : data) {
            if(d.name.equals(name)) {
                return d.generator;
            }
        }
        throw new UnknownInstructionException("Instruction with name '" + name + "' not found");
    }

    public IBytecodeStorage createStorage() {
        return new BytecodeData();
    }

}
