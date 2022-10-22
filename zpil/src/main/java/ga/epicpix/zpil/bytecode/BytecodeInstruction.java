package ga.epicpix.zpil.bytecode;

import ga.epicpix.zpil.bytecode.Bytecode.BytecodeInstructionData;
import ga.epicpix.zprol.structures.IBytecodeInstruction;

import java.util.Arrays;
import java.util.Objects;

final class BytecodeInstruction implements IBytecodeInstruction {
    private final BytecodeInstructionData data;
    private final Object[] args;

    BytecodeInstruction(BytecodeInstructionData data, Object[] args) {
        this.data = data;
        this.args = args;
    }

    public BytecodeInstructionData data() {
        return data;
    }

    public String toString() {
        return getName() + (args.length != 0 ? " " + Arrays.toString(args).replace("\n", "\\n").replace("\0", "\\0") : "");
    }

    public int getId() {
        return data.id;
    }

    public String getName() {
        return data.name;
    }

    public Object[] getData() {
        return args;
    }

}
