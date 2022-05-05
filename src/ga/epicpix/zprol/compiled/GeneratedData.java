package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.bytecode.IBytecodeInstruction;
import ga.epicpix.zprol.exceptions.compilation.FunctionNotDefinedException;
import ga.epicpix.zprol.exceptions.compilation.RedefinedFunctionException;

import java.io.*;
import java.util.ArrayList;

import static ga.epicpix.zprol.StaticImports.createStorage;

public class GeneratedData {

    public final ArrayList<Function> functions = new ArrayList<>();
    public final ConstantPool constantPool = new ConstantPool();

    public GeneratedData addCompiled(CompiledData data) {
        for(Function f : data.getFunctions()) {
            for(Function validate : functions) {
                if(validate.namespace() != null && f.namespace() != null && !validate.namespace().equals(f.namespace())) continue;
                if(!validate.name().equals(f.name())) continue;

                if(validate.signature().validateFunctionSignature(f.signature())) {
                    throw new RedefinedFunctionException((f.namespace() != null ? f.namespace() + "." : "") + f.name() + " - " + f.signature());
                }
            }
            functions.add(f);
            constantPool.getOrCreateFunctionIndex(f);
            f.prepareConstantPool(constantPool);
        }
        return this;
    }

    public GeneratedData addCompiled(CompiledData... data) {
        for(CompiledData d : data) {
            addCompiled(d);
        }
        return this;
    }

    public Function getFunction(String namespace, String name, String signature) {
        for(Function func : functions) {
            if(func.namespace() != null && namespace != null && !func.namespace().equals(namespace)) continue;
            if(!func.name().equals(name)) continue;

            if(func.signature().toString().equals(signature)) {
                return func;
            }
        }
        throw new FunctionNotDefinedException((namespace != null ? namespace + "." : "") + name + " - " + signature);
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
                    instruction.write(out, data.constantPool);
                }
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
            Function function = new Function(namespace, modifiers, name, FunctionSignature.fromDescriptor(signature), hasCode ? createStorage() : null);
            if(hasCode) {
                function.code().setLocalsSize(in.readInt());
                int instructionsLength = in.readInt();
                for (int j = 0; j < instructionsLength; j++) {
                    function.code().pushInstruction(IBytecodeInstruction.read(in, data.constantPool));
                }
            }
            data.functions.add(function);
        }
        for(var func : data.functions) {
            if(!FunctionModifiers.isEmptyCode(func.modifiers())) {
                for (var instr : func.code().getInstructions()) {
                    IBytecodeInstruction.postRead(instr, data);
                }
            }
        }
        in.close();
        return data;
    }

}
