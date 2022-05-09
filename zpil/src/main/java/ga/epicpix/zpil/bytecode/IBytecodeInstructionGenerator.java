package ga.epicpix.zpil.bytecode;

import ga.epicpix.zprol.structures.IBytecodeInstruction;

@FunctionalInterface
public interface IBytecodeInstructionGenerator {

    public IBytecodeInstruction createInstruction(Object... args);

}
