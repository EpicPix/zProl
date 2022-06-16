package ga.epicpix.zpil.bytecode;

import ga.epicpix.zprol.structures.IBytecodeInstruction;
import ga.epicpix.zprol.structures.IBytecodeStorage;

public interface IBytecode {

    public String getInstructionPrefix(int size);

    public IBytecodeInstructionGenerator getInstruction(int id);
    public BytecodeValueType[] getInstructionValueTypesRequirements(int id);
    public IBytecodeInstructionGenerator getInstruction(String name);

    public default IBytecodeInstruction getConstructedInstruction(int id, Object... args) {
        return getInstruction(id).createInstruction(args);
    }

    public default IBytecodeInstruction getConstructedInstruction(String name, Object... args) {
        return getInstruction(name).createInstruction(args);
    }

    public IBytecodeStorage createStorage();

}
