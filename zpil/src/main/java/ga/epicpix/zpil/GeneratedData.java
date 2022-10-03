package ga.epicpix.zpil;

import ga.epicpix.zpil.attr.Attribute;
import ga.epicpix.zpil.attr.ConstantValueAttribute;
import ga.epicpix.zpil.bytecode.Bytecode;
import ga.epicpix.zpil.exceptions.FunctionNotDefinedException;
import ga.epicpix.zpil.pool.ConstantPool;
import ga.epicpix.zpil.pool.ConstantPoolEntry;
import ga.epicpix.zprol.data.ConstantValue;
import ga.epicpix.zprol.structures.*;
import ga.epicpix.zprol.structures.Class;
import ga.epicpix.zprol.types.Types;

import java.io.*;
import java.util.ArrayList;
import java.util.Objects;

public class GeneratedData {

    public final ArrayList<Function> functions = new ArrayList<>();
    public final ArrayList<Field> fields = new ArrayList<>();
    public final ArrayList<Class> classes = new ArrayList<>();
    public final ConstantPool constantPool = new ConstantPool();

    public Function getFunction(String namespace, String name, String signature) {
        for(Function func : functions) {
            if(Objects.equals(func.namespace(), name)) continue;
            if(!func.name().equals(name)) continue;

            if(func.signature().toString().equals(signature)) {
                return func;
            }
        }
        throw new FunctionNotDefinedException((namespace != null ? namespace + "." : "") + name + " - " + signature);
    }

    public Method getMethod(String namespace, String className, String name, String signature) {
        Class clazz = getClass(namespace, className);
        for(Method method : clazz.methods()) {
            if(!method.name().equals(name)) continue;

            if(method.signature().toString().equals(signature)) {
                return method;
            }
        }
        throw new FunctionNotDefinedException((namespace != null ? namespace + "." : "") + className + "." + name + " - " + signature);
    }

    public Object getField(String namespace, String name, String type) {
        for(Field fld : fields) {
            if(Objects.equals(fld.namespace(), name)) continue;
            if(!fld.name().equals(name)) continue;

            if(type.equals(fld.type().getDescriptor())) {
                return fld;
            }
        }
        throw new IllegalArgumentException("Field not found " + (namespace != null ? namespace + "." : "") + name + " - " + type);
    }

    public Class getClass(String namespace, String name) {
        for(Class clz : classes) {
            if(clz.namespace() != null && namespace != null && !clz.namespace().equals(namespace)) continue;
            if(!clz.name().equals(name)) continue;

            return clz;
        }
        throw new FunctionNotDefinedException((namespace != null ? namespace + "." : "") + name);
    }

    public static byte[] save(GeneratedData data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeBytes("zPrl");

        out.writeInt(data.constantPool.entries.size() + 1);
        for(ConstantPoolEntry entry : data.constantPool.entries) entry.write(out);

        out.writeInt(data.functions.size());
        for(Function func : data.functions) {
            out.writeInt(data.constantPool.getFunctionIndex(func));
            var hasCode = !FunctionModifiers.isEmptyCode(func.modifiers());
            if(hasCode) {
                out.writeInt(func.code().getLocalsSize());
                var instructions = func.code().getInstructions();
                out.writeInt(instructions.size());
                for (var instruction : instructions) {
                    out.write(Bytecode.write(instruction, data.constantPool));
                }
            }
        }

        out.writeInt(data.classes.size());
        for(var clz : data.classes) {
            out.writeInt(data.constantPool.getClassIndex(clz));
            out.writeInt(clz.fields().length);
            for(var field : clz.fields()) {
                out.writeInt(data.constantPool.getStringIndex(field.name()));
                out.writeInt(data.constantPool.getStringIndex(field.type().getDescriptor()));
            }
            out.writeInt(clz.methods().length);
            for(var func : clz.methods()) {
                out.writeInt(data.constantPool.getMethodIndex(func));
                var hasCode = !FunctionModifiers.isEmptyCode(func.modifiers());
                if(hasCode) {
                    out.writeInt(func.code().getLocalsSize());
                    var instructions = func.code().getInstructions();
                    out.writeInt(instructions.size());
                    for (var instruction : instructions) {
                        out.write(Bytecode.write(instruction, data.constantPool));
                    }
                }
            }
        }

        out.writeInt(data.fields.size());
        for(Field field : data.fields) {
            out.writeInt(data.constantPool.getFieldIndex(field));

            int attributeCount = 0;
            if(field.defaultValue() != null) attributeCount++;
            out.writeInt(attributeCount);

            if(field.defaultValue() != null) {
                out.write(new ConstantValueAttribute(field.defaultValue().value()).write(data.constantPool));
            }
        }

        out.close();
        return bytes.toByteArray();
    }

    public static GeneratedData load(byte[] b) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(b));
        GeneratedData data = new GeneratedData();

        if(!new String(in.readNBytes(4)).equals("zPrl")) {
            throw new IllegalStateException("invalid magic");
        }

        int length = in.readInt() - 1;
        for(int i = 0; i<length; i++) data.constantPool.entries.add(ConstantPoolEntry.read(in));

        int functionLength = in.readInt();
        for(int i = 0; i<functionLength; i++) {
            var entry = (ConstantPoolEntry.FunctionEntry) data.constantPool.entries.get(in.readInt() - 1);
            var namespace = entry.getNamespace() != 0 ? ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(entry.getNamespace() - 1)).getString() : null;
            var name = ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(entry.getName() - 1)).getString();
            var signature = ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(entry.getSignature() - 1)).getString();
            var modifiers = FunctionModifiers.getModifiers(entry.getModifiers());
            var hasCode = !FunctionModifiers.isEmptyCode(modifiers);
            Function function = new Function(namespace, modifiers, name, FunctionSignature.fromDescriptor(signature), hasCode ? Bytecode.BYTECODE.createStorage() : null);
            if(hasCode) {
                function.code().setLocalsSize(in.readInt());
                int instructionsLength = in.readInt();
                for (int j = 0; j < instructionsLength; j++) {
                    function.code().pushInstruction(Bytecode.read(in));
                }
            }
            data.functions.add(function);
        }

        int classLength = in.readInt();
        for(int i = 0; i<classLength; i++) {
            var entry = (ConstantPoolEntry.ClassEntry) data.constantPool.entries.get(in.readInt() - 1);
            var namespace = entry.getNamespace() != 0 ? ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(entry.getNamespace() - 1)).getString() : null;
            var name = ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(entry.getName() - 1)).getString();
            var fields = new ClassField[in.readInt()];
            for(int fieldIndex = 0; fieldIndex<fields.length; fieldIndex++) {
                var fieldName = ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(in.readInt() - 1)).getString();
                var fieldTypeDescriptor = ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(in.readInt() - 1)).getString();
                var fieldType = Types.getTypeFromDescriptor(fieldTypeDescriptor);
                fields[fieldIndex] = new ClassField(fieldName, fieldType);
            }
            var methods = new Method[in.readInt()];
            for(int methodIndex = 0; methodIndex<methods.length; methodIndex++) {
                var mentry = (ConstantPoolEntry.MethodEntry) data.constantPool.entries.get(in.readInt() - 1);
                var mnamespace = mentry.getNamespace() != 0 ? ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(mentry.getNamespace() - 1)).getString() : null;
                var mclassName = ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(mentry.getClassName() - 1)).getString();
                var mname = ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(mentry.getName() - 1)).getString();
                var msignature = ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(mentry.getSignature() - 1)).getString();
                var mmodifiers = FunctionModifiers.getModifiers(mentry.getModifiers());
                var mhasCode = !FunctionModifiers.isEmptyCode(mmodifiers);
                Method method = new Method(mnamespace, mmodifiers, mclassName, mname, FunctionSignature.fromDescriptor(msignature), mhasCode ? Bytecode.BYTECODE.createStorage() : null);
                if(mhasCode) {
                    method.code().setLocalsSize(in.readInt());
                    int instructionsLength = in.readInt();
                    for (int j = 0; j < instructionsLength; j++) {
                        method.code().pushInstruction(Bytecode.read(in));
                    }
                }
                methods[methodIndex] = method;
            }
            data.classes.add(new Class(namespace, name, fields, methods));
        }

        int fieldLength = in.readInt();
        for(int i = 0; i<fieldLength; i++) {
            var entry = (ConstantPoolEntry.FieldEntry) data.constantPool.entries.get(in.readInt() - 1);
            var namespace = entry.getNamespace() != 0 ? ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(entry.getNamespace() - 1)).getString() : null;
            var name = ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(entry.getName() - 1)).getString();
            var type = ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(entry.getType() - 1)).getString();
            var modifiers = FieldModifiers.getModifiers(entry.getModifiers());

            int attrCount = in.readInt();
            ConstantValue constantValue = null;
            for(int j = 0; j<attrCount; j++) {
                var attr = Attribute.read(in, data.constantPool);
                if(attr instanceof ConstantValueAttribute cvalue) {
                    constantValue = new ConstantValue(cvalue.getValue());
                }
            }

            data.fields.add(new Field(namespace, modifiers, name, Types.getTypeFromDescriptor(type), constantValue));
        }

        for(var func : data.functions) {
            if(!FunctionModifiers.isEmptyCode(func.modifiers())) {
                for (var instr : func.code().getInstructions()) {
                    Bytecode.postRead(instr, data);
                }
            }
        }
        for(var clz : data.classes) {
            for (var mth : clz.methods()) {
                if (!FunctionModifiers.isEmptyCode(mth.modifiers())) {
                    for (var instr : mth.code().getInstructions()) {
                        Bytecode.postRead(instr, data);
                    }
                }
            }
        }
        in.close();
        return data;
    }


    public void printZpil() {
        System.out.println("Functions:");
        for(var func : functions) {
            System.out.println("  Function");
            System.out.println("    Namespace: \"" + (func.namespace() != null ? func.namespace() : "") + "\"");
            System.out.println("    Name: \"" + func.name() + "\"");
            System.out.println("    Signature: \"" + func.signature() + "\"");
            System.out.println("    Modifiers (" + func.modifiers().size() + "):");
            for(var modifier : func.modifiers()) {
                System.out.println("      " + modifier);
            }
            if(!FunctionModifiers.isEmptyCode(func.modifiers())) {
                System.out.println("    Code");
                System.out.println("      Locals Size: " + func.code().getLocalsSize());
                System.out.println("      Instructions");
                for(var instruction : func.code().getInstructions()) {
                    System.out.println("        " + instruction);
                }
            }
        }

        System.out.println("Fields:");
        for(var fld : fields) {
            System.out.println("  Field");
            System.out.println("    Namespace: \"" + (fld.namespace() != null ? fld.namespace() : "") + "\"");
            System.out.println("    Name: \"" + fld.name() + "\"");
            System.out.println("    Type: \"" + fld.type().getDescriptor() + "\"");
            if(fld.defaultValue() != null) {
                if(fld.defaultValue().value() != null) {
                    System.out.println("    Constant Value: " + fld.defaultValue().value().getClass().getSimpleName() + " " + fld.defaultValue().value());
                }else {
                    System.out.println("    Constant Value: null");
                }
            }
            System.out.println("    Modifiers (" + fld.modifiers().size() + "):");
            for(var modifier : fld.modifiers()) {
                System.out.println("      " + modifier);
            }
        }

        System.out.println("Classes:");
        for(var clz : classes) {
            System.out.println("  Class");
            System.out.println("    Namespace: \"" + (clz.namespace() != null ? clz.namespace() : "") + "\"");
            System.out.println("    Name: \"" + clz.name() + "\"");
            System.out.println("    Fields:");
            for(var fld : clz.fields()) {
                System.out.println("      Field");
                System.out.println("        Name: \"" + fld.name() + "\"");
                System.out.println("        Type: \"" + fld.type().getDescriptor() + "\"");
            }
            System.out.println("    Methods:");
            for(var func : clz.methods()) {
                System.out.println("      Method");
                System.out.println("        Namespace: \"" + (func.namespace() != null ? func.namespace() : "") + "\"");
                System.out.println("        Class: \"" + func.className() + "\"");
                System.out.println("        Name: \"" + func.name() + "\"");
                System.out.println("        Signature: \"" + func.signature() + "\"");
                System.out.println("        Modifiers (" + func.modifiers().size() + "):");
                for(var modifier : func.modifiers()) {
                    System.out.println("          " + modifier);
                }
                if(!FunctionModifiers.isEmptyCode(func.modifiers())) {
                    System.out.println("        Code");
                    System.out.println("          Locals Size: " + func.code().getLocalsSize());
                    System.out.println("          Instructions");
                    for(var instruction : func.code().getInstructions()) {
                        System.out.println("            " + instruction);
                    }
                }
            }
        }
    }

}
