package ga.epicpix.zpil.bytecode;

import ga.epicpix.zpil.bytecode.Bytecode.BytecodeInstructionData;
import ga.epicpix.zprol.structures.IBytecodeInstruction;

import java.util.Arrays;

record BytecodeInstruction(BytecodeInstructionData data, Object[] args) implements IBytecodeInstruction {

    public String toString() {
        return getName() + (args.length != 0 ? " " + Arrays.toString(args).replace("\n", "\\n").replace("\0", "\\0") : "");
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

}
