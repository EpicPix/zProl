package ga.epicpix.zprol;

import ga.epicpix.zprol.bytecode.BytecodeValueType;
import ga.epicpix.zprol.bytecode.IBytecode;
import ga.epicpix.zprol.bytecode.IBytecodeInstruction;
import ga.epicpix.zprol.bytecode.IBytecodeInstructionGenerator;
import ga.epicpix.zprol.bytecode.IBytecodeStorage;
import ga.epicpix.zprol.bytecode.impl.Bytecode;

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

    public static IBytecodeInstruction getConstructedSizeInstruction(int size, String name, Object... args) {
        return getConstructedInstruction(getInstructionPrefix(size) + name, args);
    }

    public static IBytecodeStorage createStorage() {
        return bytecodeImplementation.createStorage();
    }

}
