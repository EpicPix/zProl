package ga.epicpix.zprol.compiler.compiled;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zpil.bytecode.Bytecode;
import ga.epicpix.zpil.exceptions.FunctionNotDefinedException;
import ga.epicpix.zprol.compiler.exceptions.UnknownTypeException;
import ga.epicpix.zprol.compiler.precompiled.PreCompiledData;
import ga.epicpix.zprol.compiler.exceptions.RedefinedClassException;
import ga.epicpix.zprol.compiler.exceptions.RedefinedFunctionException;
import ga.epicpix.zprol.structures.*;
import ga.epicpix.zprol.structures.Class;
import ga.epicpix.zprol.types.ClassType;
import ga.epicpix.zprol.types.Type;
import ga.epicpix.zprol.types.Types;

import java.util.ArrayList;
import java.util.Objects;

public class CompiledData {

    public final String namespace;
    private final ArrayList<PreCompiledData> using = new ArrayList<>();

    public CompiledData(String namespace) {
        this.namespace = namespace;
    }

    public void using(PreCompiledData using) {
        this.using.add(using);
    }

    public ArrayList<PreCompiledData> getUsing() {
        return using;
    }

    private final ArrayList<Function> functions = new ArrayList<>();
    private final ArrayList<Class> classes = new ArrayList<>();

    public ArrayList<Function> getFunctions() {
        return new ArrayList<>(functions);
    }

    public ArrayList<Class> getClasses() {
        return new ArrayList<>(classes);
    }

    public void includeToGenerated(GeneratedData data) {
        for(Function f : functions) {
            for(Function validate : data.functions) {
                if(!Objects.equals(validate.namespace(), f.namespace())) continue;
                if(!validate.name().equals(f.name())) continue;

                if(validate.signature().validateFunctionSignature(f.signature())) {
                    throw new RedefinedFunctionException((f.namespace() != null ? f.namespace() + "." : "") + f.name() + " - " + f.signature());
                }
            }
            data.functions.add(f);
            data.constantPool.getOrCreateFunctionIndex(f);
            if(!FunctionModifiers.isEmptyCode(f.modifiers())) {
                for (var instruction : f.code().getInstructions()) {
                    Bytecode.prepareConstantPool(instruction, data.constantPool);
                }
            }
        }

        for(Class clz : classes) {
            for(Class validate : data.classes) {
                if(!Objects.equals(validate.namespace(), clz.namespace())) continue;
                if(!validate.name().equals(clz.name())) continue;

                throw new RedefinedClassException((clz.namespace() != null ? clz.namespace() + "." : "") + clz.name());
            }
            data.classes.add(clz);
            data.constantPool.getOrCreateClassIndex(clz);
            data.constantPool.getOrCreateStringIndex(clz.namespace());
            data.constantPool.getOrCreateStringIndex(clz.name());
            for(ClassField field : clz.fields()) {
                data.constantPool.getOrCreateStringIndex(field.name());
                data.constantPool.getOrCreateStringIndex(field.type().getDescriptor());
            }
        }
    }

    public Function getFunction(String namespace, String name, FunctionSignature sig) {
        for(Function func : functions) {
            if(!func.namespace().equals(namespace)) continue;
            if(!func.name().equals(name)) continue;

            var signature = func.signature();
            if(sig.returnType() != null && signature.returnType() != sig.returnType()) continue;
            var sigParams = sig.parameters();
            if(signature.parameters().length != sigParams.length) continue;

            boolean success = true;
            for(int i = 0; i<signature.parameters().length; i++) {
                if(signature.parameters()[i] != sigParams[i]) {
                    success = false;
                    break;
                }
            }
            if(success) {
                return func;
            }
        }
        throw new FunctionNotDefinedException(name);
    }

    public void addFunction(Function function) {
        functions.add(function);
    }

    public void addClass(Class clazz) {
        classes.add(clazz);
    }

    public Type resolveType(String type) {
        var t = Types.getType(type);
        if(t != null) return t;
        String namespace = type.lastIndexOf('.') != -1 ? type.substring(0, type.lastIndexOf('.')) : null;
        String name = type.lastIndexOf('.') != -1 ? type.substring(type.lastIndexOf('.') + 1) : type;
        for(var data : using) {
            if(namespace == null) {
                if(!Objects.equals(data.namespace, null) && !Objects.equals(data.namespace, "zprol.lang")) continue;
            }else {
                if (data.namespace != null && !data.namespace.equals(namespace)) continue;
            }
            for(var clz : data.classes) {
                if(clz.name.equals(name)) {
                    return new ClassType(data.namespace, clz.name);
                }
            }
        }
        throw new UnknownTypeException(type);
    }

}