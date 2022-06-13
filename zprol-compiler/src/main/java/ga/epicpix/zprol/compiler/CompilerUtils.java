package ga.epicpix.zprol.compiler;

import ga.epicpix.zpil.bytecode.Bytecode;
import ga.epicpix.zpil.bytecode.IBytecodeInstructionGenerator;
import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.precompiled.PreClass;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.structures.IBytecodeInstruction;
import ga.epicpix.zprol.structures.IBytecodeStorage;
import ga.epicpix.zprol.types.ClassType;

import java.math.BigInteger;
import java.util.Objects;

public class CompilerUtils {

    public static PreClass classTypeToPreClass(ClassType type, CompiledData data) {
        for(var use : data.getUsing()) {
            for(var clz : use.classes) {
                if(!Objects.equals(clz.namespace, type.getNamespace())) continue;
                if(!clz.name.equals(type.getName())) continue;
                return clz;
            }
        }
        return null;
    }

    public static BigInteger getDecimalInteger(Token token) {
        try {
            return new BigInteger(token.toStringRaw(), 10);
        }catch(NumberFormatException e) {
            throw new TokenLocatedException("Decimal Integer not a valid integer '" + token.toStringRaw() + "'", token);
        }
    }

    public static String getInstructionPrefix(int size) {
        return Bytecode.BYTECODE.getInstructionPrefix(size);
    }

    public static IBytecodeInstructionGenerator getInstruction(String name) {
        return Bytecode.BYTECODE.getInstruction(name);
    }

    public static IBytecodeInstruction getConstructedInstruction(int id, Object... args) {
        return Bytecode.BYTECODE.getConstructedInstruction(id, args);
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
