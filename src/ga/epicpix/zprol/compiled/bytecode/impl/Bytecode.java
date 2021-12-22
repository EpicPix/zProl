package ga.epicpix.zprol.compiled.bytecode.impl;

import ga.epicpix.zprol.compiled.bytecode.BytecodeValueType;
import ga.epicpix.zprol.compiled.bytecode.IBytecode;
import ga.epicpix.zprol.compiled.bytecode.IBytecodeInstructionGenerator;
import java.util.ArrayList;

public final class Bytecode implements IBytecode {

    public static final IBytecode BYTECODE = new Bytecode();

    static record BytecodeInstructionData(int id, String name, BytecodeValueType[] values, IBytecodeInstructionGenerator generator) {}

    private final ArrayList<BytecodeInstructionData> data = new ArrayList<>();

    private Bytecode() {

    }

    public void registerInstruction(int id, String name, BytecodeValueType... values) {
        for(BytecodeInstructionData d : data) {
            if(d.id == id) {
                throw new IllegalArgumentException("Instruction with id '" + d.id + "' already registered with name '" + d.name + "'");
            }else if(d.name.equals(name)) {
                throw new IllegalArgumentException("Instruction with name '" + d.name + "' already registered with id '" + d.id + "'");
            }
        }
        data.add(new BytecodeInstructionData(id, name, values, (args) -> {
            for(BytecodeInstructionData d : data) {
                if(d.id == id) {
                    if(d.values.length != args.length) throw new IllegalArgumentException("Invalid amount of arguments (" + args.length + "), expected " + d.values.length);

                    return new BytecodeInstruction(d, args);
                }
            }
            throw new IllegalStateException("Data about instruction not found (" + id + ")");
        }));
    }

    public IBytecodeInstructionGenerator getInstruction(int id) {
        for(BytecodeInstructionData d : data) {
            if(d.id == id) {
                return d.generator;
            }
        }
        throw new IllegalArgumentException("Instruction with id '" + id + "' not found");
    }

    public IBytecodeInstructionGenerator getInstruction(String name) {
        for(BytecodeInstructionData d : data) {
            if(d.name.equals(name)) {
                return d.generator;
            }
        }
        throw new IllegalArgumentException("Instruction with name '" + name + "' not found");
    }

    public byte[] writeInstructions() {
        return null;
    }

}
