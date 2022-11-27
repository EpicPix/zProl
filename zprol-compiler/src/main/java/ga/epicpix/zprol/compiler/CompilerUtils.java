package ga.epicpix.zprol.compiler;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zpil.bytecode.Bytecode;
import ga.epicpix.zprol.compiler.compiled.CompiledData;
import ga.epicpix.zprol.compiler.precompiled.*;
import ga.epicpix.zprol.structures.*;
import ga.epicpix.zprol.structures.Class;
import ga.epicpix.zprol.types.ClassType;
import ga.epicpix.zprol.types.Type;

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
        for(GeneratedData use : data.getAllGenerated()) {
            for(Class clz : use.classes) {
                if(!Objects.equals(clz.namespace, type.getNamespace())) continue;
                if(!clz.name.equals(type.getName())) continue;
                PreClass c = new PreClass();
                c.namespace = clz.namespace;
                c.name = clz.name;
                for(ClassField f : clz.fields) {
                    PreField a = new PreField(null);
                    a.name = f.name;
                    a.type = f.type.normalName();
                    c.fields.add(a);
                }
                for(Method m : clz.methods) {
                    PreFunction f = new PreFunction();
                    f.name = m.name;
                    f.returnType = m.signature.returnType.normalName();
                    for(Type t : m.signature.parameters) {
                        PreParameter p = new PreParameter();
                        p.type = t.normalName();
                        f.parameters.add(p);
                    }
                    c.methods.add(f);
                }
                return c;
            }
        }
        throw new RuntimeException("ClassType to PreClass tried to return null");
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
