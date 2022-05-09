package ga.epicpix.zpil;

import ga.epicpix.zpil.bytecode.Bytecode;
import ga.epicpix.zpil.exceptions.FunctionNotDefinedException;
import ga.epicpix.zpil.pool.ConstantPool;
import ga.epicpix.zpil.pool.ConstantPoolEntry;
import ga.epicpix.zprol.structures.*;
import ga.epicpix.zprol.structures.Class;
import ga.epicpix.zprol.types.Types;

import java.io.*;
import java.util.ArrayList;
import java.util.Objects;

public class GeneratedData {

    public final ArrayList<Function> functions = new ArrayList<>();
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
        for(Class clz : data.classes) {
            out.writeInt(data.constantPool.getClassIndex(clz));
            out.writeInt(clz.fields().length);
            for(ClassField field : clz.fields()) {
                out.writeInt(data.constantPool.getStringIndex(field.name()));
                out.writeInt(data.constantPool.getStringIndex(field.type().getDescriptor()));
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
            data.classes.add(new Class(namespace, name, fields));
        }

        for(var func : data.functions) {
            if(!FunctionModifiers.isEmptyCode(func.modifiers())) {
                for (var instr : func.code().getInstructions()) {
                    Bytecode.postRead(instr, data);
                }
            }
        }
        in.close();
        return data;
    }

}
