package ga.epicpix.zprol.compiled.bytecode;

@FunctionalInterface
public interface IBytecodeInstructionGenerator {

    public IBytecodeInstruction createInstruction(Object... args);

}
