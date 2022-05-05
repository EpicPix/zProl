package ga.epicpix.zprol.bytecode;

@FunctionalInterface
public interface IBytecodeInstructionGenerator {

    public IBytecodeInstruction createInstruction(Object... args);

}
