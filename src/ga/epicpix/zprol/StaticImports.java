package ga.epicpix.zprol;

import ga.epicpix.zprol.compiled.bytecode.BytecodeValueType;
import ga.epicpix.zprol.compiled.bytecode.IBytecode;
import ga.epicpix.zprol.compiled.bytecode.IBytecodeInstruction;
import ga.epicpix.zprol.compiled.bytecode.IBytecodeInstructionGenerator;
import ga.epicpix.zprol.compiled.bytecode.IBytecodeStorage;
import ga.epicpix.zprol.compiled.bytecode.impl.Bytecode;

public class StaticImports {

    public static IBytecode bytecodeImplementation = Bytecode.BYTECODE;

    public static void registerInstruction(int id, String name, BytecodeValueType... values) {
        bytecodeImplementation.registerInstruction(id, name, values);
    }

    public static String getInstructionPrefix(int size) {
        return bytecodeImplementation.getInstructionPrefix(size);
    }

    public static IBytecodeInstructionGenerator getInstruction(int id) {
        return bytecodeImplementation.getInstruction(id);
    }

    public static BytecodeValueType[] getInstructionValueTypesRequirements(int id) {
        return bytecodeImplementation.getInstructionValueTypesRequirements(id);
    }

    public static IBytecodeInstructionGenerator getInstruction(String name) {
        return bytecodeImplementation.getInstruction(name);
    }

    public static IBytecodeInstruction getConstructedInstruction(int id, Object... args) {
        return bytecodeImplementation.getConstructedInstruction(id, args);
    }

    public static IBytecodeInstruction getConstructedInstruction(String name, Object... args) {
        return bytecodeImplementation.getConstructedInstruction(name, args);
    }

    public static IBytecodeStorage createStorage() {
        return bytecodeImplementation.createStorage();
    }

}
