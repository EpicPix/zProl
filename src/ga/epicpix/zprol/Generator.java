package ga.epicpix.zprol;

import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.compiled.Flag;
import ga.epicpix.zprol.compiled.Function;
import ga.epicpix.zprol.compiled.Type;
import ga.epicpix.zprol.compiled.TypeFunctionSignature;
import ga.epicpix.zprol.compiled.TypeFunctionSignatureNamed;
import ga.epicpix.zprol.compiled.TypeNamed;
import ga.epicpix.zprol.compiled.Types;
import ga.epicpix.zprol.compiled.bytecode.Bytecode;
import ga.epicpix.zprol.compiled.bytecode.BytecodeInstruction;
import ga.epicpix.zprol.compiled.bytecode.BytecodeInstructions;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Generator {

    public static void generate_x86_64_linux_assembly(CompiledData data, File save) throws IOException {

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

        for(Function func : data.getFunctions()) {
            if(!func.flags.contains(Flag.NO_IMPLEMENTATION)) {
                boolean flipNot = false;
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
                TypeFunctionSignatureNamed sig = func.signature;
                int current = 0;
                for(int i = 0; i<sig.parameters.length; i++) {
                    Types type = sig.parameters[i].type.type;
                    int size = type.memorySize;
                    current += size;
                    if(size == 1) writer.write("    mov byte [rbp-" + current + "], " + parameters[i][3] + "\n");
                    else if(size == 2) writer.write("    mov word [rbp-" + current + "], " + parameters[i][2] + "\n");
                    else if(size == 4) writer.write("    mov dword [rbp-" + current + "], " + parameters[i][1] + "\n");
                    else if(size == 8) writer.write("    mov qword [rbp-" + current + "], " + parameters[i][0] + "\n");
                }
                int instrIndex = 0;
                for(BytecodeInstruction instr : bc.getInstructions()) {
                    writer.write(funcName + "@" + instrIndex + ":   ; " + instr.instruction.name().toLowerCase() + "\n");
                    if(instr.instruction == BytecodeInstructions.PUSHI8) {
                        writer.write("    dec rsp\n");
                        writer.write("    mov byte [rsp], " + instr.data[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.PUSHI16) {
                        writer.write("    push word " + instr.data[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.PUSHI32) {
                        writer.write("    sub rsp, 4\n");
                        writer.write("    mov dword [rsp], " + instr.data[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.PUSHI64) {
                        long l = (long) instr.data[0];
                        if(l < 0x00000000ffffffffL && l >= 0) {
                            writer.write("    push " + l + "\n");
                        }else {
                            writer.write("    sub rsp, 4\n");
                            writer.write("    mov dword [rsp], " + (l & 0x00000000ffffffffL) + "\n");
                            writer.write("    sub rsp, 4\n");
                            writer.write("    mov dword [rsp], " + ((l & 0xffffffff00000000L) >> 32) + "\n");
                        }
                    }else if(instr.instruction == BytecodeInstructions.EX8T16) {
                        writer.write("    mov byte al, [rsp]\n");
                        writer.write("    inc rsp\n");
                        writer.write("    cbw\n");
                        writer.write("    sub rsp, 2\n");
                        writer.write("    mov word [rsp], ax\n");
                    }else if(instr.instruction == BytecodeInstructions.EX8T32) {
                        writer.write("    mov byte al, [rsp]\n");
                        writer.write("    inc rsp\n");
                        writer.write("    cbw\n");
                        writer.write("    cwde\n");
                        writer.write("    sub rsp, 4\n");
                        writer.write("    mov dword [rsp], eax\n");
                    }else if(instr.instruction == BytecodeInstructions.EX8T64) {
                        writer.write("    mov byte al, [rsp]\n");
                        writer.write("    inc rsp\n");
                        writer.write("    cbw\n");
                        writer.write("    cwde\n");
                        writer.write("    cdqe\n");
                        writer.write("    sub rsp, 8\n");
                        writer.write("    mov qword [rsp], rax\n");
                    }else if(instr.instruction == BytecodeInstructions.EX16T8) {
                        writer.write("    mov word ax, [rsp]\n");
                        writer.write("    add rsp, 2\n");
                        writer.write("    dec rsp\n");
                        writer.write("    mov byte [rsp], al\n");
                    }else if(instr.instruction == BytecodeInstructions.EX16T32) {
                        writer.write("    mov word ax, [rsp]\n");
                        writer.write("    add rsp, 2\n");
                        writer.write("    cwde\n");
                        writer.write("    sub rsp, 4\n");
                        writer.write("    mov dword [rsp], eax\n");
                    }else if(instr.instruction == BytecodeInstructions.EX16T64) {
                        writer.write("    mov word ax, [rsp]\n");
                        writer.write("    add rsp, 2\n");
                        writer.write("    cwde\n");
                        writer.write("    cdqe\n");
                        writer.write("    sub rsp, 8\n");
                        writer.write("    mov qword [rsp], rax\n");
                    }else if(instr.instruction == BytecodeInstructions.EX32T8) {
                        writer.write("    mov dword eax, [rsp]\n");
                        writer.write("    add rsp, 4\n");
                        writer.write("    dec rsp\n");
                        writer.write("    mov byte [rsp], al\n");
                    }else if(instr.instruction == BytecodeInstructions.EX32T16) {
                        writer.write("    mov dword eax, [rsp]\n");
                        writer.write("    add rsp, 4\n");
                        writer.write("    sub rsp, 2\n");
                        writer.write("    mov dword [rsp], ax\n");
                    }else if(instr.instruction == BytecodeInstructions.EX32T64) {
                        writer.write("    mov dword eax, [rsp]\n");
                        writer.write("    add rsp, 4\n");
                        writer.write("    cdqe\n");
                        writer.write("    sub rsp, 8\n");
                        writer.write("    mov qword [rsp], rax\n");
                    }else if(instr.instruction == BytecodeInstructions.EX64T8) {
                        writer.write("    mov qword rax, [rsp]\n");
                        writer.write("    add rsp, 8\n");
                        writer.write("    dec rsp\n");
                        writer.write("    mov byte [rsp], al\n");
                    }else if(instr.instruction == BytecodeInstructions.EX64T16) {
                        writer.write("    mov qword rax, [rsp]\n");
                        writer.write("    add rsp, 8\n");
                        writer.write("    sub rsp, 2\n");
                        writer.write("    mov word [rsp], ax\n");
                    }else if(instr.instruction == BytecodeInstructions.EX64T32) {
                        writer.write("    mov qword rax, [rsp]\n");
                        writer.write("    add rsp, 8\n");
                        writer.write("    sub rsp, 4\n");
                        writer.write("    mov dword [rsp], eax\n");
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
                    }else if(instr.instruction == BytecodeInstructions.LOAD8) {
                        writer.write("    mov byte al, [rbp-" + instr.data[0] + "],\n");
                        writer.write("    dec rsp\n");
                        writer.write("    mov byte [rsp], al\n");
                    }else if(instr.instruction == BytecodeInstructions.LOAD16) {
                        writer.write("    mov word ax, [rbp-" + instr.data[0] + "],\n");
                        writer.write("    push ax\n");
                    }else if(instr.instruction == BytecodeInstructions.LOAD32) {
                        writer.write("    mov dword eax, [rbp-" + instr.data[0] + "],\n");
                        writer.write("    sub rsp, 4\n");
                        writer.write("    mov dword [rsp], eax\n");
                    }else if(instr.instruction == BytecodeInstructions.LOAD64) {
                        writer.write("    mov qword rax, [rbp-" + instr.data[0] + "]\n");
                        writer.write("    sub rsp, 8\n");
                        writer.write("    mov qword [rsp], rax\n");
                    }else if(instr.instruction == BytecodeInstructions.ADD8) {
                        writer.write("    mov byte al, [rsp]\n");
                        writer.write("    inc rsp\n");
                        writer.write("    mov byte ah, [rsp]\n");
                        writer.write("    inc rsp\n");
                        writer.write("    add al, ah\n");
                        writer.write("    dec rsp\n");
                        writer.write("    mov byte [rsp], al\n");
                    }else if(instr.instruction == BytecodeInstructions.ADD16) {
                        writer.write("    pop ax\n");
                        writer.write("    pop bx\n");
                        writer.write("    add ax, bx\n");
                        writer.write("    push ax\n");
                    }else if(instr.instruction == BytecodeInstructions.ADD32) {
                        writer.write("    mov dword eax, [rsp]\n");
                        writer.write("    add rsp, 4\n");
                        writer.write("    mov dword ebx, [rsp]\n");
                        writer.write("    add rsp, 4\n");
                        writer.write("    add eax, ebx\n");
                        writer.write("    sub rsp, 4\n");
                        writer.write("    mov dword [rsp], eax\n");
                    }else if(instr.instruction == BytecodeInstructions.ADD64) {
                        writer.write("    mov qword rax, [rsp]\n");
                        writer.write("    add rsp, 8\n");
                        writer.write("    mov qword rbx, [rsp]\n");
                        writer.write("    add rsp, 8\n");
                        writer.write("    add rax, rbx\n");
                        writer.write("    sub rsp, 8\n");
                        writer.write("    mov qword [rsp], rax\n");
                    }else if(instr.instruction == BytecodeInstructions.AND8) {
                        writer.write("    mov byte al, [rsp]\n");
                        writer.write("    inc rsp\n");
                        writer.write("    mov byte ah, [rsp]\n");
                        writer.write("    inc rsp\n");
                        writer.write("    and al, ah\n");
                        writer.write("    dec rsp\n");
                        writer.write("    mov byte [rsp], al\n");
                    }else if(instr.instruction == BytecodeInstructions.AND16) {
                        writer.write("    pop ax\n");
                        writer.write("    pop bx\n");
                        writer.write("    and ax, bx\n");
                        writer.write("    push ax\n");
                    }else if(instr.instruction == BytecodeInstructions.AND32) {
                        writer.write("    mov dword eax, [rsp]\n");
                        writer.write("    add rsp, 4\n");
                        writer.write("    mov dword ebx, [rsp]\n");
                        writer.write("    add rsp, 4\n");
                        writer.write("    and eax, ebx\n");
                        writer.write("    sub rsp, 4\n");
                        writer.write("    mov dword [rsp], eax\n");
                    }else if(instr.instruction == BytecodeInstructions.AND64) {
                        writer.write("    mov qword rax, [rsp]\n");
                        writer.write("    add rsp, 8\n");
                        writer.write("    mov qword rbx, [rsp]\n");
                        writer.write("    add rsp, 8\n");
                        writer.write("    and rax, rbx\n");
                        writer.write("    sub rsp, 8\n");
                        writer.write("    mov qword [rsp], rax\n");
                    }else if(instr.instruction == BytecodeInstructions.COMPARE64) {
                        writer.write("    mov qword rax, [rsp]\n");
                        writer.write("    add rsp, 8\n");
                        writer.write("    mov qword rbx, [rsp]\n");
                        writer.write("    add rsp, 8\n");
                        writer.write("    cmp rax, rbx\n");
                    }else if(instr.instruction == BytecodeInstructions.COMPAREN64) {
                        writer.write("    mov qword rax, [rsp]\n");
                        writer.write("    add rsp, 8\n");
                        writer.write("    mov qword rbx, [rsp]\n");
                        writer.write("    add rsp, 8\n");
                        writer.write("    cmp rax, rbx\n");
                        flipNot = !flipNot;
                    }else if(instr.instruction == BytecodeInstructions.JUMPNE) {
                        if(!flipNot) {
                            writer.write("    jne " + funcName + "@" + (instrIndex + (short) instr.data[0]) + "\n");
                        }else {
                            writer.write("    je " + funcName + "@" + (instrIndex + (short) instr.data[0]) + "\n");
                        }
                        flipNot = false;
                    }else if(instr.instruction == BytecodeInstructions.JUMPE) {
                        if(!flipNot) {
                            writer.write("    je " + funcName + "@" + (instrIndex + (short) instr.data[0]) + "\n");
                        }else {
                            writer.write("    jne " + funcName + "@" + (instrIndex + (short) instr.data[0]) + "\n");
                        }
                        flipNot = false;
                    }else if(instr.instruction == BytecodeInstructions.JUMP) {
                        writer.write("    jmp " + funcName + "@" + (instrIndex + (short) instr.data[0]) + "\n");
                    }else if(instr.instruction == BytecodeInstructions.PUSHFUNCTION) {
                        Function f = data.getFunctions().get((short) instr.data[0]);
                        StringBuilder zfuncName = new StringBuilder(f.name);
                        if(!f.name.equals("_start")) {
                            zfuncName.append(".").append(f.signature.returnType);
                            for(TypeNamed param : f.signature.parameters) {
                                zfuncName.append(".").append(param.type.type.toString().toLowerCase());
                            }
                        }
                        writer.write("    sub rsp, 8\n");
                        writer.write("    mov qword [rsp], " + zfuncName + "\n");
                    }else if(instr.instruction == BytecodeInstructions.INVOKESTATIC) {
                        Function f = data.getFunctions().get((short) instr.data[0]);
                        StringBuilder zfuncName = new StringBuilder(f.name);
                        if(!f.name.equals("_start")) {
                            zfuncName.append(".").append(f.signature.returnType);
                            for(TypeNamed param : f.signature.parameters) {
                                zfuncName.append(".").append(param.type.type.toString().toLowerCase());
                            }
                        }
                        TypeFunctionSignatureNamed s = f.signature;
                        for(int i = 0; i<s.parameters.length; i++) {
                            TypeNamed param = s.parameters[i];
                            int size = param.type.type.memorySize;
                            if(size == 1) {
                                writer.write("    mov byte " + parameters[i][3] + ", [rsp]\n");
                                writer.write("    inc rsp\n");
                            } else if(size == 2) {
                                writer.write("    mov word " + parameters[i][2] + ", [rsp]\n");
                                writer.write("    add rsp, 2\n");
                            } else if(size == 4) {
                                writer.write("    mov dword " + parameters[i][1] + ", [rsp]\n");
                                writer.write("    add rsp, 4\n");
                            } else if(size == 8) {
                                writer.write("    mov qword " + parameters[i][0] + ", [rsp]\n");
                                writer.write("    add rsp, 8\n");
                            }
                        }
                        writer.write("    call " + zfuncName + "\n");
                        int retSize = s.returnType.type.memorySize;
                        if(retSize == 1) {
                            writer.write("    dec rsp\n");
                            writer.write("    mov byte [rsp], " + parameters[0][3] + "\n");
                        }else if(retSize == 2) {
                            writer.write("    sub rsp, 2\n");
                            writer.write("    mov word [rsp], " + parameters[0][2] + "\n");
                        }else if(retSize == 4) {
                            writer.write("    sub rsp, 4\n");
                            writer.write("    mov dword [rsp], " + parameters[0][1] + "\n");
                        }else if(retSize == 8) {
                            writer.write("    sub rsp, 8\n");
                            writer.write("    mov qword [rsp], " + parameters[0][0] + "\n");
                        }
                    }else if(instr.instruction == BytecodeInstructions.INVOKESIGNATURE) {
                        TypeFunctionSignature fsig = (TypeFunctionSignature) instr.data[0];
                        writer.write("    mov qword r15, [rsp]\n");
                        writer.write("    add rsp, 8\n");
                        for(int i = 0; i<fsig.parameters.length; i++) {
                            Type param = fsig.parameters[i];
                            int size = param.type.memorySize;
                            if(size == 1) {
                                writer.write("    mov byte " + parameters[i][3] + ", [rsp]\n");
                                writer.write("    inc rsp\n");
                            } else if(size == 2) {
                                writer.write("    mov word " + parameters[i][2] + ", [rsp]\n");
                                writer.write("    add rsp, 2\n");
                            } else if(size == 4) {
                                writer.write("    mov dword " + parameters[i][1] + ", [rsp]\n");
                                writer.write("    add rsp, 4\n");
                            } else if(size == 8) {
                                writer.write("    mov qword " + parameters[i][0] + ", [rsp]\n");
                                writer.write("    add rsp, 8\n");
                            }
                        }
                        writer.write("    call r15\n");
                        int retSize = fsig.returnType.type.memorySize;
                        if(retSize == 1) {
                            writer.write("    dec rsp\n");
                            writer.write("    mov byte [rsp], " + parameters[0][3] + "\n");
                        }else if(retSize == 2) {
                            writer.write("    sub rsp, 2\n");
                            writer.write("    mov word [rsp], " + parameters[0][2] + "\n");
                        }else if(retSize == 4) {
                            writer.write("    sub rsp, 4\n");
                            writer.write("    mov dword [rsp], " + parameters[0][1] + "\n");
                        }else if(retSize == 8) {
                            writer.write("    sub rsp, 8\n");
                            writer.write("    mov qword [rsp], " + parameters[0][0] + "\n");
                        }
                    }else if(instr.instruction == BytecodeInstructions.SYSCALL1) {
                        writer.write("    pop " + calls[1] + "\n");
                        writer.write("    syscall\n");
                        writer.write("    push " + calls[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.SYSCALL2) {
                        writer.write("    pop " + calls[2] + "\n");
                        writer.write("    pop " + calls[1] + "\n");
                        writer.write("    syscall\n");
                        writer.write("    push " + calls[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.SYSCALL3) {
                        writer.write("    pop " + calls[3] + "\n");
                        writer.write("    pop " + calls[2] + "\n");
                        writer.write("    pop " + calls[1] + "\n");
                        writer.write("    syscall\n");
                        writer.write("    push " + calls[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.SYSCALL4) {
                        writer.write("    pop " + calls[4] + "\n");
                        writer.write("    pop " + calls[3] + "\n");
                        writer.write("    pop " + calls[2] + "\n");
                        writer.write("    pop " + calls[1] + "\n");
                        writer.write("    syscall\n");
                        writer.write("    push " + calls[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.SYSCALL5) {
                        writer.write("    pop " + calls[5] + "\n");
                        writer.write("    pop " + calls[4] + "\n");
                        writer.write("    pop " + calls[3] + "\n");
                        writer.write("    pop " + calls[2] + "\n");
                        writer.write("    pop " + calls[1] + "\n");
                        writer.write("    syscall\n");
                        writer.write("    push " + calls[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.SYSCALL6) {
                        writer.write("    pop " + calls[6] + "\n");
                        writer.write("    pop " + calls[5] + "\n");
                        writer.write("    pop " + calls[4] + "\n");
                        writer.write("    pop " + calls[3] + "\n");
                        writer.write("    pop " + calls[2] + "\n");
                        writer.write("    pop " + calls[1] + "\n");
                        writer.write("    syscall\n");
                        writer.write("    push " + calls[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.SYSCALL7) {
                        writer.write("    pop " + calls[7] + "\n");
                        writer.write("    pop " + calls[6] + "\n");
                        writer.write("    pop " + calls[5] + "\n");
                        writer.write("    pop " + calls[4] + "\n");
                        writer.write("    pop " + calls[3] + "\n");
                        writer.write("    pop " + calls[2] + "\n");
                        writer.write("    pop " + calls[1] + "\n");
                        writer.write("    syscall\n");
                        writer.write("    push " + calls[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.PUSHSTR) {
                        writer.write("    push " + funcName + ".str" + instr.data[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.POP8) {
                        writer.write("    inc rsp\n");
                    }else if(instr.instruction == BytecodeInstructions.POP16) {
                        writer.write("    add rsp, 2\n");
                    }else if(instr.instruction == BytecodeInstructions.POP32) {
                        writer.write("    add rsp, 4\n");
                    }else if(instr.instruction == BytecodeInstructions.POP64) {
                        writer.write("    add rsp, 8\n");
                    }else if(instr.instruction == BytecodeInstructions.RETURN) {
                        if(func.name.equals("_start")) {
                            writer.write("    mov " + calls[1] + ", 60\n");
                            writer.write("    mov " + calls[2] + ", 0\n");
                            writer.write("    syscall\n");
                        }else {
                            writer.write("    leave\n");
                            writer.write("    ret\n");
                        }
                    }else if(instr.instruction == BytecodeInstructions.RETURN8) {
                        if(func.name.equals("_start")) {
                            writer.write("    mov byte dil, [rsp]\n");
                            writer.write("    mov " + calls[1] + ", 60\n");
                            writer.write("    syscall\n");
                        }else {
                            writer.write("    mov byte al, [rsp]\n");
                            writer.write("    inc rsp\n");
                            writer.write("    leave\n");
                            writer.write("    ret\n");
                        }
                    }else if(instr.instruction == BytecodeInstructions.RETURN16) {
                        if(func.name.equals("_start")) {
                            writer.write("    mov word di, [rsp]\n");
                            writer.write("    mov " + calls[1] + ", 60\n");
                            writer.write("    syscall\n");
                        }else {
                            writer.write("    mov word ax, [rsp]\n");
                            writer.write("    add rsp, 2\n");
                            writer.write("    leave\n");
                            writer.write("    ret\n");
                        }
                    }else if(instr.instruction == BytecodeInstructions.RETURN32) {
                        if(func.name.equals("_start")) {
                            writer.write("    mov dword edi, [rsp]\n");
                            writer.write("    mov " + calls[1] + ", 60\n");
                            writer.write("    syscall\n");
                        }else {
                            writer.write("    mov dword eax, [rsp]\n");
                            writer.write("    add rsp, 4\n");
                            writer.write("    leave\n");
                            writer.write("    ret\n");
                        }
                    }else if(instr.instruction == BytecodeInstructions.RETURN64) {
                        if(func.name.equals("_start")) {
                            writer.write("    mov qword rdi, [rsp]\n");
                            writer.write("    mov " + calls[1] + ", 60\n");
                            writer.write("    syscall\n");
                        }else {
                            writer.write("    mov qword rax, [rsp]\n");
                            writer.write("    add rsp, 8\n");
                            writer.write("    leave\n");
                            writer.write("    ret\n");
                        }
                    }else {
                        System.err.println("Missing instruction: " + instr);
                    }
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

                ArrayList<String> strings = bc.getStrings();
                for(int i = 0; i<strings.size(); i++) {
                    char[] chars = strings.get(i).toCharArray();
                    String[] hexs = new String[chars.length];
                    for(int j = 0; j<chars.length; j++) {
                        hexs[j] = "0x" + Integer.toHexString(chars[j]);
                    }
                    writer.write(funcName + ".str" + i + ": db " + String.join(", ", hexs) + ", 0x0\n");
                }
            }
        }

//        writer.write();

        writer.close();
    }

}
