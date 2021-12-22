package ga.epicpix.zprol.compiled.bytecode;

public interface IBytecode {

    public void registerInstruction(int id, String name, BytecodeValueType... values);

    public String getInstructionPrefix(int size);

    public IBytecodeInstructionGenerator getInstruction(int id);
    public IBytecodeInstructionGenerator getInstruction(String name);

    public default IBytecodeInstruction getConstructedInstruction(int id, Object... args) {
        return getInstruction(id).createInstruction(args);
    }

    public default IBytecodeInstruction getConstructedInstruction(String name, Object... args) {
        return getInstruction(name).createInstruction(args);
    }

    public IBytecodeStorage createStorage();

}
