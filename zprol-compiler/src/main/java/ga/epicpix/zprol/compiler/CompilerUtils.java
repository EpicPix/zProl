package ga.epicpix.zprol.compiler;

import ga.epicpix.zpil.bytecode.Bytecode;
import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.precompiled.PreClass;
import ga.epicpix.zprol.compiler.precompiled.PreCompiledData;
import ga.epicpix.zprol.structures.IBytecodeInstruction;
import ga.epicpix.zprol.structures.IBytecodeStorage;
import ga.epicpix.zprol.types.ClassType;

import java.util.Objects;

public class CompilerUtils {

    public static PreClass classTypeToPreClass(ClassType type, CompiledData data) {
        for(PreCompiledData use : data.getUsing()) {
            for(PreClass clz : use.classes) {
                if(!Objects.equals(clz.namespace, type.getNamespace())) continue;
                if(!clz.name.equals(type.getName())) continue;
                return clz;
            }
        }
        return null;
    }

    public static String getInstructionPrefix(int size) {
        return Bytecode.BYTECODE.getInstructionPrefix(size);
    }

    public static IBytecodeInstruction getConstructedInstruction(String name, Object... args) {
        return Bytecode.BYTECODE.getConstructedInstruction(name, args);
    }

    public static IBytecodeInstruction getConstructedSizeInstruction(int size, String name, Object... args) {
        return getConstructedInstruction(getInstructionPrefix(size) + name, args);
    }

    public static IBytecodeStorage createStorage() {
        return Bytecode.BYTECODE.createStorage();
    }

}
