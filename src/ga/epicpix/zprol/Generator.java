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

    public static void generate_x86_linux_assembly(CompiledData data, File save, boolean x64) throws IOException {

        final String[] calls = !x64 ? new String[]{ // 32bit
                "eax", "eax", "ebx", "ecx", "edx", "esi", "edi", "ebp"} : new String[]{ // 64bit
                "rax", "rax", "rdi", "rsi", "rdx", "r10", "r8", "r9"};
        final String syscallKeyword = !x64 ? "int 0x80" : "syscall";
        final int syscallExit = !x64 ? 1 : 60;
        final String stackPointer = !x64 ? "esp" : "rsp";
        final String basePointer = !x64 ? "ebp" : "rbp";

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
                    writer.write("    ; " + instr.instruction.name().toLowerCase() + "\n");
                    if(instr.instruction == BytecodeInstructions.PUSHI8) {
                        writer.write("    dec " + stackPointer + "\n");
                        writer.write("    mov byte [" + stackPointer + "], " + instr.data[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.PUSHI16) {
                        writer.write("    push word " + instr.data[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.PUSHI32) {
                        writer.write("    sub " + stackPointer + ", 4\n");
                        writer.write("    mov dword [" + stackPointer + "], " + instr.data[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.PUSHI64) {
                        writer.write("    sub " + stackPointer + ", 4\n");
                        writer.write("    mov dword [" + stackPointer + "], " + ((long)instr.data[0]&0x00000000ffffffffL) + "\n");
                        writer.write("    sub " + stackPointer + ", 4\n");
                        writer.write("    mov dword [" + stackPointer + "], " + (((long)instr.data[0]&0xffffffff00000000L)>>32) + "\n");
                    }else if(instr.instruction == BytecodeInstructions.STORE8) {
                        writer.write("    mov byte al, [" + stackPointer + "]\n");
                        writer.write("    inc " + stackPointer + "\n");
                        writer.write("    mov byte [" + basePointer + "-" + instr.data[0] + "], al\n");
                    }else if(instr.instruction == BytecodeInstructions.STORE16) {
                        writer.write("    pop ax\n");
                        writer.write("    mov word [" + basePointer + "-" + instr.data[0] + "], ax\n");
                    }else if(instr.instruction == BytecodeInstructions.STORE32) {
                        writer.write("    mov dword eax, [" + stackPointer + "]\n");
                        writer.write("    add " + stackPointer + ", 4\n");
                        writer.write("    mov dword [" + basePointer + "-" + instr.data[0] + "], eax\n");
                    }else if(instr.instruction == BytecodeInstructions.STORE64) {
                        if(x64) {
                            writer.write("    pop rax\n");
                            writer.write("    mov qword [" + basePointer + "-" + instr.data[0] + "], rax\n");
                        }else {
                            writer.write("    pop eax\n");
                            writer.write("    mov dword [" + basePointer + "-" + instr.data[0] + "], eax\n");
                            writer.write("    pop ebx\n");
                            writer.write("    mov dword [" + basePointer + "-" + ((short) instr.data[0] - 4) + "], ebx\n");
                        }
                    }else if(instr.instruction == BytecodeInstructions.LOAD8) {
                        writer.write("    mov byte al, [" + basePointer + "-" + instr.data[0] + "],\n");
                        writer.write("    dec " + stackPointer + "\n");
                        writer.write("    mov byte [" + stackPointer + "], al\n");
                    }else if(instr.instruction == BytecodeInstructions.LOAD16) {
                        writer.write("    mov word ax, [" + basePointer + "-" + instr.data[0] + "],\n");
                        writer.write("    push ax\n");
                    }else if(instr.instruction == BytecodeInstructions.LOAD32) {
                        writer.write("    mov dword eax, [" + basePointer + "-" + instr.data[0] + "],\n");
                        writer.write("    sub " + stackPointer + ", 4\n");
                        writer.write("    mov dword [" + stackPointer + "], eax\n");
                    }else if(instr.instruction == BytecodeInstructions.LOAD64) {
                        if(x64) {
                            writer.write("    mov qword rax, [" + basePointer + "-" + instr.data[0] + "]\n");
                            writer.write("    sub " + stackPointer + ", 8\n");
                            writer.write("    mov qword [" + stackPointer + "], rax\n");
                        }else {
                            writer.write("    mov dword eax, [" + basePointer + "-" + ((short) instr.data[0] - 4) + "]\n");
                            writer.write("    sub " + stackPointer + ", 4\n");
                            writer.write("    mov dword [" + stackPointer + "], eax\n");
                            writer.write("    mov dword eax, [" + basePointer + "-" + instr.data[0] + "]\n");
                            writer.write("    sub " + stackPointer + ", 4\n");
                            writer.write("    mov dword [" + stackPointer + "], eax\n");
                        }
                    }else if(instr.instruction == BytecodeInstructions.ADD8) {
                        writer.write("    mov byte al, [" + stackPointer + "]\n");
                        writer.write("    inc " + stackPointer + "\n");
                        writer.write("    mov byte ah, [" + stackPointer + "]\n");
                        writer.write("    inc " + stackPointer + "\n");
                        writer.write("    add al, ah\n");
                        writer.write("    dec " + stackPointer + "\n");
                        writer.write("    mov byte [" + stackPointer + "], al\n");
                    }else if(instr.instruction == BytecodeInstructions.ADD16) {
                        writer.write("    pop ax\n");
                        writer.write("    pop bx\n");
                        writer.write("    add ax, bx\n");
                        writer.write("    push ax\n");
                    }else if(instr.instruction == BytecodeInstructions.ADD32) {
                        writer.write("    mov dword eax, [" + stackPointer + "]\n");
                        writer.write("    add " + stackPointer + ", 4\n");
                        writer.write("    mov dword ebx, [" + stackPointer + "]\n");
                        writer.write("    add " + stackPointer + ", 4\n");
                        writer.write("    add eax, ebx\n");
                        writer.write("    sub " + stackPointer + ", 4\n");
                        writer.write("    mov dword [" + stackPointer + "], eax\n");
                    }else if(instr.instruction == BytecodeInstructions.ADD64) {
                        if(x64) {
                            writer.write("    mov qword rax, [" + stackPointer + "]\n");
                            writer.write("    add " + stackPointer + ", 8\n");
                            writer.write("    mov qword rbx, [" + stackPointer + "]\n");
                            writer.write("    add " + stackPointer + ", 8\n");
                            writer.write("    add rax, rbx\n");
                            writer.write("    sub " + stackPointer + ", 8\n");
                            writer.write("    mov qword [" + stackPointer + "], rax\n");
                        }else {
                            writer.write("    mov dword eax, [" + stackPointer + "]\n");
                            writer.write("    add " + stackPointer + ", 4\n");
                            writer.write("    mov dword ebx, [" + stackPointer + "]\n");
                            writer.write("    add " + stackPointer + ", 4\n");
                            writer.write("    mov dword ecx, [" + stackPointer + "]\n");
                            writer.write("    add " + stackPointer + ", 4\n");
                            writer.write("    mov dword edx, [" + stackPointer + "]\n");
                            writer.write("    add " + stackPointer + ", 4\n");
                            writer.write("    add eax, ecx\n");
                            writer.write("    sub " + stackPointer + ", 4\n");
                            writer.write("    mov dword [" + stackPointer + "], eax\n");
                            writer.write("    adc ebx, edx\n");
                            writer.write("    sub " + stackPointer + ", 4\n");
                            writer.write("    mov dword [" + stackPointer + "], eax\n");
                        }
                    }else {
                        System.err.println("Missing instruction: " + instr);
                    }
                }
                if(func.name.equals("_start")) {
                    writer.write("    ; exit\n");
                    writer.write("    mov " + calls[1] + ", " + syscallExit + "\n");
                    writer.write("    mov " + calls[2] + ", 0\n");
                    writer.write("    " + syscallKeyword + "\n");
                }else {
                    writer.write("    ; return\n");
                    writer.write("    ret\n");
                }
            }
        }

//        writer.write();

        writer.close();
    }

}
