package ga.epicpix.zprol.compiler.compiled;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zpil.attr.ConstantValueAttribute;
import ga.epicpix.zpil.bytecode.Bytecode;
import ga.epicpix.zpil.exceptions.FunctionNotDefinedException;
import ga.epicpix.zprol.compiler.exceptions.RedefinedFieldException;
import ga.epicpix.zprol.compiler.exceptions.UnknownTypeException;
import ga.epicpix.zprol.compiler.precompiled.PreCompiledData;
import ga.epicpix.zprol.compiler.exceptions.RedefinedClassException;
import ga.epicpix.zprol.compiler.exceptions.RedefinedFunctionException;
import ga.epicpix.zprol.structures.*;
import ga.epicpix.zprol.structures.Class;
import ga.epicpix.zprol.types.ArrayType;
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

    public final ArrayList<Function> functions = new ArrayList<>();
    public final ArrayList<Field> fields = new ArrayList<>();
    public final ArrayList<Class> classes = new ArrayList<>();

    public void includeToGenerated(GeneratedData data) {
        for(var f : functions) {
            for(var validate : data.functions) {
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

        for(var f : fields) {
            for(var validate : data.fields) {
                if(!Objects.equals(validate.namespace(), f.namespace())) continue;
                if(!validate.name().equals(f.name())) continue;

                throw new RedefinedFieldException((f.namespace() != null ? f.namespace() + "." : "") + f.name() + " - " + f.type().normalName());

            }
            data.fields.add(f);
            data.constantPool.getOrCreateFieldIndex(f);
            if(f.defaultValue() != null) {
                new ConstantValueAttribute(f.defaultValue().value()).prepareConstantPool(data.constantPool);
            }
        }

        for(var clz : classes) {
            for(var validate : data.classes) {
                if(!Objects.equals(validate.namespace(), clz.namespace())) continue;
                if(!validate.name().equals(clz.name())) continue;

                throw new RedefinedClassException((clz.namespace() != null ? clz.namespace() + "." : "") + clz.name());
            }
            data.classes.add(clz);
            data.constantPool.getOrCreateClassIndex(clz);
            for(var field : clz.fields()) {
                data.constantPool.getOrCreateStringIndex(field.name());
                data.constantPool.getOrCreateStringIndex(field.type().getDescriptor());
            }
            for(var m : clz.methods()) {
                data.constantPool.getOrCreateMethodIndex(m);
                if(!FunctionModifiers.isEmptyCode(m.modifiers())) {
                    for (var instruction : m.code().getInstructions()) {
                        Bytecode.prepareConstantPool(instruction, data.constantPool);
                    }
                }
            }
        }
    }

    public void addFunction(Function function) {
        functions.add(function);
    }

    public void addField(Field field) {
        fields.add(field);
    }

    public void addClass(Class clazz) {
        classes.add(clazz);
    }

    public Type resolveType(String type) {
        int arrAmount = 0;
        while(type.endsWith("[]")) {
            type = type.substring(0, type.length() - 2);
            arrAmount++;
        }
        var t = Types.getType(type);
        if(t != null) {
            for(int i = 0; i<arrAmount; i++) {
                t = new ArrayType(t);
            }
            return t;
        }
        String namespace = type.lastIndexOf('.') != -1 ? type.substring(0, type.lastIndexOf('.')) : null;
        String name = type.lastIndexOf('.') != -1 ? type.substring(type.lastIndexOf('.') + 1) : type;
        for(var data : using) {
            if(namespace != null) {
                if(!Objects.equals(namespace, data.namespace)) continue;
            }
            for(var clz : data.classes) {
                if(clz.name.equals(name)) {
                    t = new ClassType(data.namespace, clz.name);
                    for(int i = 0; i<arrAmount; i++) {
                        t = new ArrayType(t);
                    }
                    return t;
                }
            }
        }
        throw new UnknownTypeException(type);
    }

}
