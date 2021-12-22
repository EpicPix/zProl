package ga.epicpix.zprol.generators;

import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.compiled.CompiledData.LinkedData;
import ga.epicpix.zprol.compiled.ConstantPoolEntry.FunctionEntry;
import ga.epicpix.zprol.compiled.Flag;
import ga.epicpix.zprol.compiled.Function;
import ga.epicpix.zprol.compiled.ObjectField;
import ga.epicpix.zprol.compiled.Type;
import ga.epicpix.zprol.compiled.FunctionSignature;
import ga.epicpix.zprol.compiled.bytecode.IBytecodeInstruction;
import ga.epicpix.zprol.compiled.bytecode.IBytecodeStorage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class GeneratorAssembly {

    public static void generate_x86_64_linux_assembly(LinkedData data, File save) throws IOException {

        final String[][] parameters = {
                {"rax", "eax", "ax", "al"},
                {"rbx", "ebx", "bx", "bl"},
                {"rcx", "rcx", "cx", "cl"},
                {"rdx", "edx", "dx", "dl"},
                {"r8", "r8d", "r8w", "r8b"},
                {"r9", "r9d", "r9w", "r9b"},
        };

        final String[] calls = { "rax", "rax", "rdi", "rsi", "rdx", "r10", "r8", "r9" };

        BufferedWriter writer = new BufferedWriter(new FileWriter(save));

        for(CompiledData compiled : data.data) {
            for(Function func : compiled.getFunctions()) {
                if(!func.flags.contains(Flag.NO_IMPLEMENTATION)) {
                    String funcName = getFullFunctionName(compiled.namespace, func.name.replace('<', '@').replace('>', '@'), func.signature);
                    if(func.name.equals("_start")) {
                        writer.write("global " + func.name + "\n");
                    }
                    writer.write(funcName + ":\n");
                    IBytecodeStorage bc = func.code;

//                    writer.write("    enter " + bc.getLocalsSize() + ", 0\n"); TODO
                    FunctionSignature sig = func.signature;
                    int current = 0;
                    for(int i = 0; i < sig.parameters.length; i++) {
                        int size = sig.parameters[i].getSize();
                        current += size;
                        if(size == 1) writer.write("    mov byte [rbp-" + current + "], " + parameters[i][3] + "\n");
                        else if(size == 2) writer.write("    mov word [rbp-" + current + "], " + parameters[i][2] + "\n");
                        else if(size == 4) writer.write("    mov dword [rbp-" + current + "], " + parameters[i][1] + "\n");
                        else if(size == 8) writer.write("    mov qword [rbp-" + current + "], " + parameters[i][0] + "\n");
                    }
                    int instrIndex = 0;
                    for(IBytecodeInstruction instr : bc.getInstructions()) {
                        writer.write(funcName + "@" + instrIndex + ":   ; " + instr.getName() + "\n");
                        System.err.println("Missing instruction: " + instr);
                        instrIndex++;
                    }
                    writer.write(funcName + "@" + instrIndex + ":\n");
                    if(func.name.equals("_start")) {
                        writer.write("    ; exit\n");
                        writer.write("    mov " + calls[1] + ", 60\n");
                        writer.write("    mov " + calls[2] + ", 0\n");
                        writer.write("    syscall\n");
                    } else {
                        writer.write("    ; return\n");
                        writer.write("    leave\n");
                        writer.write("    ret\n");
                    }
                }
            }

            //data.getFields().get((short)instr.data[0]).name

            writer.write(".bss:\n");
            for(ObjectField field : compiled.getFields()) {
                writer.write(field.name + ": resb " + field.type.getSize() + "\n");
            }
        }

//        writer.write();

        writer.close();
    }

    public static String getFullFunctionName(String namespace, String name, FunctionSignature signature) {
        if(name.equals("_start")) return "_start";
        StringBuilder zfuncName = new StringBuilder();
        zfuncName.append(namespace).append(".").append(name).append("$").append(signature.returnType.getName().toLowerCase());
        for(Type param : signature.parameters) zfuncName.append(".").append(param.getName().toLowerCase());
        return zfuncName.toString();
    }

}
