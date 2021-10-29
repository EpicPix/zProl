package ga.epicpix.zprol;

import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.compiled.Flag;
import ga.epicpix.zprol.compiled.Function;
import ga.epicpix.zprol.compiled.TypeNamed;
import ga.epicpix.zprol.compiled.bytecode.Bytecode;
import ga.epicpix.zprol.compiled.bytecode.BytecodeInstruction;
import ga.epicpix.zprol.compiled.bytecode.BytecodeInstructions;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Generator {

    public static void generate_x86_64_linux_assembly(CompiledData data, File save) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(save));

        for(Function func : data.getFunctions()) {
            if(!func.flags.contains(Flag.NO_IMPLEMENTATION)) {
                StringBuilder funcName = new StringBuilder(func.name);
                if(!func.name.equals("_start")) {
                    funcName.append(".").append(func.signature.returnType);
                    for(TypeNamed param : func.signature.parameters) {
                        funcName.append(".").append(param.type.type.toString().toLowerCase());
                    }
                }else {
                    writer.write("global " + func.name + "\n");
                }
                writer.write(funcName + ":\n");
                Bytecode bc = func.code;
                writer.write("    enter " + bc.getLocalVariablesSize() + ", 0\n");
                for(BytecodeInstruction instr : bc.getInstructions()) {
                    if(instr.instruction == BytecodeInstructions.PUSHI8) {
                        writer.write("    dec rsp\n");
                        writer.write("    mov byte [rsp], " + instr.data[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.PUSHI16) {
                        writer.write("    push word " + instr.data[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.PUSHI32) {
                        writer.write("    sub rsp, 4\n");
                        writer.write("    mov dword [rsp], " + instr.data[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.PUSHI64) {
                        writer.write("    sub rsp, 8\n");
                        writer.write("    mov qword [rsp], " + instr.data[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.STORE8) {
                        writer.write("    mov byte al, [rsp]\n");
                        writer.write("    inc rsp\n");
                        writer.write("    mov byte [rbp-" + instr.data[0] + "], al\n");
                    }else if(instr.instruction == BytecodeInstructions.STORE16) {
                        writer.write("    pop ax\n");
                        writer.write("    mov word [rbp-" + instr.data[0] + "], ax\n");
                    }else if(instr.instruction == BytecodeInstructions.STORE32) {
                        writer.write("    mov dword eax, [rsp]\n");
                        writer.write("    add rsp, 4\n");
                        writer.write("    mov dword [rbp-" + instr.data[0] + "], eax\n");
                    }else if(instr.instruction == BytecodeInstructions.STORE64) {
                        writer.write("    pop rax\n");
                        writer.write("    mov qword [rbp-" + instr.data[0] + "], rax\n");
                    }else {
                        System.err.println("Missing instruction: " + instr);
                    }
                }
                writer.write("    mov rax, 60\n");
                writer.write("    mov rdi, 0\n");
                writer.write("    syscall\n");
            }
        }

//        writer.write();

        writer.close();
    }

}
