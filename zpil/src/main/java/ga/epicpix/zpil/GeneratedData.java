package ga.epicpix.zpil;

import ga.epicpix.zpil.attr.Attribute;
import ga.epicpix.zpil.attr.ConstantValueAttribute;
import ga.epicpix.zpil.bytecode.Bytecode;
import ga.epicpix.zpil.pool.ConstantPool;
import ga.epicpix.zpil.pool.ConstantPoolEntry;
import ga.epicpix.zprol.data.ConstantValue;
import ga.epicpix.zprol.structures.*;
import ga.epicpix.zprol.structures.Class;
import ga.epicpix.zprol.types.Types;

import java.io.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public class GeneratedData {

    public final ArrayList<Function> functions = new ArrayList<>();
    public final ArrayList<Field> fields = new ArrayList<>();
    public final ArrayList<Class> classes = new ArrayList<>();
    public final ConstantPool constantPool = new ConstantPool();

    public static byte[] save(GeneratedData data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeBytes("zPrl");

        out.writeInt(data.constantPool.entries.size() + 1);
        for(ConstantPoolEntry entry : data.constantPool.entries) entry.write(out);

        out.writeInt(data.functions.size());
        for(Function func : data.functions) {
            out.writeInt(data.constantPool.getStringIndex(func.namespace));
            out.writeInt(data.constantPool.getStringIndex(func.name));
            out.writeInt(data.constantPool.getStringIndex(func.signature.toString()));
            out.writeInt(FunctionModifiers.toBits(func.modifiers));
            boolean hasCode = !FunctionModifiers.isEmptyCode(func.modifiers);
            if(hasCode) {
                out.writeInt(func.code.getLocalsSize());
                List<IBytecodeInstruction> instructions = func.code.getInstructions();
                out.writeInt(instructions.size());
                for (IBytecodeInstruction instruction : instructions) {
                    Bytecode.write(instruction, data, out);
                }
            }
        }

        out.writeInt(data.classes.size());
        for(Class clz : data.classes) {
            out.writeInt(data.constantPool.getStringIndex(clz.namespace));
            out.writeInt(data.constantPool.getStringIndex(clz.name));
            out.writeInt(clz.fields.length);
            for(ClassField field : clz.fields) {
                out.writeInt(data.constantPool.getStringIndex(field.name));
                out.writeInt(data.constantPool.getStringIndex(field.type.getDescriptor()));
            }
            out.writeInt(clz.methods.length);
            for(Method func : clz.methods) {
                out.writeInt(data.constantPool.getStringIndex(func.name));
                out.writeInt(data.constantPool.getStringIndex(func.signature.toString()));
                out.writeInt(FunctionModifiers.toBits(func.modifiers));
                boolean hasCode = !FunctionModifiers.isEmptyCode(func.modifiers);
                if(hasCode) {
                    out.writeInt(func.code.getLocalsSize());
                    List<IBytecodeInstruction> instructions = func.code.getInstructions();
                    out.writeInt(instructions.size());
                    for (IBytecodeInstruction instruction : instructions) {
                        Bytecode.write(instruction, data, out);
                    }
                }
            }
        }

        out.writeInt(data.fields.size());
        for(Field field : data.fields) {
            out.writeInt(data.constantPool.getStringIndex(field.namespace));
            out.writeInt(data.constantPool.getStringIndex(field.name));
            out.writeInt(data.constantPool.getStringIndex(field.type.getDescriptor()));
            out.writeInt(FieldModifiers.toBits(field.modifiers));

            int attributeCount = 0;
            if(field.defaultValue != null) attributeCount++;
            out.writeInt(attributeCount);

            if(field.defaultValue != null) {
                out.write(new ConstantValueAttribute(field.defaultValue.value).write(data.constantPool));
            }
        }

        out.close();
        return bytes.toByteArray();
    }

    public static GeneratedData load(byte[] b) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(b));
        GeneratedData data = new GeneratedData();

        if(in.readInt() != 0x7a50726c) {
            throw new IllegalStateException("invalid magic");
        }

        int length = in.readInt() - 1;
        for(int i = 0; i<length; i++) data.constantPool.entries.add(ConstantPoolEntry.read(in));

        int functionLength = in.readInt();
        for(int i = 0; i<functionLength; i++) {
            String namespace = data.constantPool.getStringNullable(in.readInt());
            String name = data.constantPool.getString(in.readInt());
            String signature = data.constantPool.getString(in.readInt());
            EnumSet<FunctionModifiers> modifiers = FunctionModifiers.getModifiers(in.readInt());
            boolean hasCode = !FunctionModifiers.isEmptyCode(modifiers);
            Function function = new Function(namespace, modifiers, name, FunctionSignature.fromDescriptor(signature), hasCode ? Bytecode.BYTECODE.createStorage() : null);
            if(hasCode) {
                function.code.setLocalsSize(in.readInt());
                int instructionsLength = in.readInt();
                for (int j = 0; j < instructionsLength; j++) {
                    function.code.pushInstruction(Bytecode.read(in));
                }
            }
            data.functions.add(function);
        }

        int classLength = in.readInt();
        for(int i = 0; i<classLength; i++) {
            String namespace = data.constantPool.getStringNullable(in.readInt());
            String name = data.constantPool.getString(in.readInt());
            ClassField[] fields = new ClassField[in.readInt()];
            for(int fieldIndex = 0; fieldIndex<fields.length; fieldIndex++) {
                String fieldName = data.constantPool.getString(in.readInt());
                String fieldTypeDescriptor = data.constantPool.getString(in.readInt());
                ga.epicpix.zprol.types.Type fieldType = Types.getTypeFromDescriptor(fieldTypeDescriptor);
                fields[fieldIndex] = new ClassField(fieldName, fieldType);
            }
            Method[] methods = new Method[in.readInt()];
            for(int methodIndex = 0; methodIndex<methods.length; methodIndex++) {
                String mname = data.constantPool.getStringNullable(in.readInt());
                String msignature = data.constantPool.getString(in.readInt());
                EnumSet<FunctionModifiers> mmodifiers = FunctionModifiers.getModifiers(in.readInt());
                boolean mhasCode = !FunctionModifiers.isEmptyCode(mmodifiers);
                Method method = new Method(namespace, mmodifiers, name, mname, FunctionSignature.fromDescriptor(msignature), mhasCode ? Bytecode.BYTECODE.createStorage() : null);
                if(mhasCode) {
                    method.code.setLocalsSize(in.readInt());
                    int instructionsLength = in.readInt();
                    for (int j = 0; j < instructionsLength; j++) {
                        method.code.pushInstruction(Bytecode.read(in));
                    }
                }
                methods[methodIndex] = method;
            }
            data.classes.add(new Class(namespace, name, fields, methods));
        }

        int fieldLength = in.readInt();
        for(int i = 0; i<fieldLength; i++) {
            String namespace = data.constantPool.getStringNullable(in.readInt());
            String name =  data.constantPool.getString(in.readInt());
            String type = data.constantPool.getString(in.readInt());
            EnumSet<FieldModifiers> modifiers = FieldModifiers.getModifiers(in.readInt());

            int attrCount = in.readInt();
            ConstantValue constantValue = null;
            for(int j = 0; j<attrCount; j++) {
                Attribute attr = Attribute.read(in, data.constantPool);
                if(attr instanceof ConstantValueAttribute) {
                    constantValue = new ConstantValue(((ConstantValueAttribute) attr).getValue());
                }
            }

            data.fields.add(new Field(namespace, modifiers, name, Types.getTypeFromDescriptor(type), constantValue));
        }

        for(Function func : data.functions) {
            if(!FunctionModifiers.isEmptyCode(func.modifiers)) {
                for (IBytecodeInstruction instr : func.code.getInstructions()) {
                    Bytecode.postRead(instr, data);
                }
            }
        }
        for(Class clz : data.classes) {
            for (Method mth : clz.methods) {
                if (!FunctionModifiers.isEmptyCode(mth.modifiers)) {
                    for (IBytecodeInstruction instr : mth.code.getInstructions()) {
                        Bytecode.postRead(instr, data);
                    }
                }
            }
        }
        in.close();
        return data;
    }

    public Function getFunction(FunctionSignature sig, String name) {
        for(Function func : functions) {
            if(!func.signature.equals(sig)) continue;
            if(!func.name.equals(name)) continue;
            return func;
        }
        return null;
    }

    public void includeToGenerated(GeneratedData data) {
        for(Function f : functions) {
            for(Function validate : data.functions) {
                if(!Objects.equals(validate.namespace, f.namespace)) continue;
                if(!validate.name.equals(f.name)) continue;

                if(validate.signature.validateFunctionSignature(f.signature)) {
                    throw new RuntimeException((f.namespace != null ? f.namespace + "." : "") + f.name + " - " + f.signature);
                }
            }
            data.functions.add(f);
            data.constantPool.prepareConstantPool(f);
            if(!FunctionModifiers.isEmptyCode(f.modifiers)) {
                for (IBytecodeInstruction instruction : f.code.getInstructions()) {
                    Bytecode.prepareConstantPool(instruction, data.constantPool);
                }
            }
        }

        for(Field f : fields) {
            for(Field validate : data.fields) {
                if(!Objects.equals(validate.namespace, f.namespace)) continue;
                if(!validate.name.equals(f.name)) continue;

                throw new RuntimeException((f.namespace != null ? f.namespace + "." : "") + f.name + " - " + f.type.normalName());

            }
            data.fields.add(f);
            data.constantPool.prepareConstantPool(f);
            if(f.defaultValue != null) {
                new ConstantValueAttribute(f.defaultValue.value).prepareConstantPool(data.constantPool);
            }
        }

        for(Class clz : classes) {
            for(Class validate : data.classes) {
                if(!Objects.equals(validate.namespace, clz.namespace)) continue;
                if(!validate.name.equals(clz.name)) continue;

                throw new RuntimeException((clz.namespace != null ? clz.namespace + "." : "") + clz.name);
            }
            data.classes.add(clz);
            data.constantPool.prepareConstantPool(clz);
            for(ClassField field : clz.fields) {
                data.constantPool.getOrCreateStringIndex(field.name);
                data.constantPool.getOrCreateStringIndex(field.type.getDescriptor());
            }
            for(Method m : clz.methods) {
                data.constantPool.prepareConstantPool(m);
                if(!FunctionModifiers.isEmptyCode(m.modifiers)) {
                    for (IBytecodeInstruction instruction : m.code.getInstructions()) {
                        Bytecode.prepareConstantPool(instruction, data.constantPool);
                    }
                }
            }
        }
    }

    public void printZpil() {
        System.out.println("Functions:");
        for(Function func : functions) {
            System.out.println("  Function");
            System.out.println("    Namespace: \"" + (func.namespace != null ? func.namespace : "") + "\"");
            System.out.println("    Name: \"" + func.name + "\"");
            System.out.println("    Signature: \"" + func.signature + "\"");
            System.out.println("    Modifiers (" + func.modifiers.size() + "):");
            for(FunctionModifiers modifier : func.modifiers) {
                System.out.println("      " + modifier);
            }
            if(!FunctionModifiers.isEmptyCode(func.modifiers)) {
                System.out.println("    Code");
                System.out.println("      Locals Size: " + func.code.getLocalsSize());
                int len = 0;
                for(IBytecodeInstruction instruction : func.code.getInstructions()) {
                    len += instruction.getSize();
                }
                System.out.println("      Instructions (" + len + " " + (len == 1 ? "byte":"bytes") + ")");
                for(IBytecodeInstruction instruction : func.code.getInstructions()) {
                    System.out.println("        " + instruction);
                }
            }
        }

        System.out.println("Fields:");
        for(Field fld : fields) {
            System.out.println("  Field");
            System.out.println("    Namespace: \"" + (fld.namespace != null ? fld.namespace : "") + "\"");
            System.out.println("    Name: \"" + fld.name + "\"");
            System.out.println("    Type: \"" + fld.type.getDescriptor() + "\"");
            if(fld.defaultValue != null) {
                if(fld.defaultValue.value != null) {
                    System.out.println("    Constant Value: " + fld.defaultValue.value.getClass().getSimpleName() + " " + fld.defaultValue.value);
                }else {
                    System.out.println("    Constant Value: null");
                }
            }
            System.out.println("    Modifiers (" + fld.modifiers.size() + "):");
            for(FieldModifiers modifier : fld.modifiers) {
                System.out.println("      " + modifier);
            }
        }

        System.out.println("Classes:");
        for(Class clz : classes) {
            System.out.println("  Class");
            System.out.println("    Namespace: \"" + (clz.namespace != null ? clz.namespace : "") + "\"");
            System.out.println("    Name: \"" + clz.name + "\"");
            System.out.println("    Fields:");
            for(ClassField fld : clz.fields) {
                System.out.println("      Field");
                System.out.println("        Name: \"" + fld.name + "\"");
                System.out.println("        Type: \"" + fld.type.getDescriptor() + "\"");
            }
            System.out.println("    Methods:");
            for(Method func : clz.methods) {
                System.out.println("      Method");
                System.out.println("        Namespace: \"" + (func.namespace != null ? func.namespace : "") + "\"");
                System.out.println("        Class: \"" + func.className + "\"");
                System.out.println("        Name: \"" + func.name + "\"");
                System.out.println("        Signature: \"" + func.signature + "\"");
                System.out.println("        Modifiers (" + func.modifiers.size() + "):");
                for(FunctionModifiers modifier : func.modifiers) {
                    System.out.println("          " + modifier);
                }
                if(!FunctionModifiers.isEmptyCode(func.modifiers)) {
                    System.out.println("        Code");
                    System.out.println("          Locals Size: " + func.code.getLocalsSize());
                    int len = 0;
                    for(IBytecodeInstruction instruction : func.code.getInstructions()) {
                        len += instruction.getSize();
                    }
                    System.out.println("          Instructions (" + len + " " + (len == 1 ? "byte":"bytes") + ")");
                    for(IBytecodeInstruction instruction : func.code.getInstructions()) {
                        System.out.println("            " + instruction);
                    }
                }
            }
        }
    }

}
