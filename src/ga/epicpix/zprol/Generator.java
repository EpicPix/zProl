package ga.epicpix.zprol;

import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.compiled.Flag;
import ga.epicpix.zprol.compiled.Function;
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

    public static void generate_x86_linux_assembly(CompiledData data, File save, boolean x64) throws IOException {

        final String[][] parameters = {
                {"rax", "eax", "ax", "al"},
                {"rbx", "ebx", "bx", "bl"},
                {"rcx", "rcx", "cx", "cl"},
                {"rdx", "edx", "dx", "dl"},
                {"r8", "r8d", "r8w", "r8b"},
                {"r9", "r9d", "r9w", "r9b"},
        };

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
                boolean runRet = true;
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
                    if(size == 1) writer.write("    mov byte [" + basePointer + "-" + current + "], " + parameters[i][3] + "\n");
                    else if(size == 2) writer.write("    mov word [" + basePointer + "-" + current + "], " + parameters[i][2] + "\n");
                    else if(size == 4) writer.write("    mov dword [" + basePointer + "-" + current + "], " + parameters[i][1] + "\n");
                    else if(size == 8) writer.write("    mov qword [" + basePointer + "-" + current + "], " + parameters[i][0] + "\n");
                }
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
                        long l = (long) instr.data[0];
                        if(x64 && l < 0x00000000ffffffffL && l >= 0) {
                            writer.write("    push " + l + "\n");
                        }else {
                            writer.write("    sub " + stackPointer + ", 4\n");
                            writer.write("    mov dword [" + stackPointer + "], " + (l & 0x00000000ffffffffL) + "\n");
                            writer.write("    sub " + stackPointer + ", 4\n");
                            writer.write("    mov dword [" + stackPointer + "], " + ((l & 0xffffffff00000000L) >> 32) + "\n");
                        }
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
                                writer.write("    mov byte " + parameters[i][3] + ", [" + stackPointer + "]\n");
                                writer.write("    inc " + stackPointer + "\n");
                            } else if(size == 2) {
                                writer.write("    mov word " + parameters[i][2] + ", [" + stackPointer + "]\n");
                                writer.write("    add " + stackPointer + ", 2\n");
                            } else if(size == 4) {
                                writer.write("    mov dword " + parameters[i][1] + ", [" + stackPointer + "]\n");
                                writer.write("    add " + stackPointer + ", 4\n");
                            } else if(size == 8) {
                                writer.write("    mov qword " + parameters[i][0] + ", [" + stackPointer + "]\n");
                                writer.write("    add " + stackPointer + ", 8\n");
                            }
                        }
                        writer.write("    call " + zfuncName + "\n");
                        int retSize = s.returnType.type.memorySize;
                        if(retSize == 1) {
                            writer.write("    dec " + stackPointer + "\n");
                            writer.write("    mov byte [" + stackPointer + "], " + parameters[0][3] + "\n");
                        }else if(retSize == 2) {
                            writer.write("    sub " + stackPointer + ", 2\n");
                            writer.write("    mov word [" + stackPointer + "], " + parameters[0][2] + "\n");
                        }else if(retSize == 4) {
                            writer.write("    sub " + stackPointer + ", 4\n");
                            writer.write("    mov dword [" + stackPointer + "], " + parameters[0][1] + "\n");
                        }else if(retSize == 8) {
                            writer.write("    sub " + stackPointer + ", 8\n");
                            writer.write("    mov qword [" + stackPointer + "], " + parameters[0][0] + "\n");
                        }
                    }else if(instr.instruction == BytecodeInstructions.SYSCALL1) {
                        writer.write("    pop " + calls[1] + "\n");
                        writer.write("    " + syscallKeyword + "\n");
                        writer.write("    push " + calls[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.SYSCALL2) {
                        writer.write("    pop " + calls[2] + "\n");
                        writer.write("    pop " + calls[1] + "\n");
                        writer.write("    " + syscallKeyword + "\n");
                        writer.write("    push " + calls[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.SYSCALL3) {
                        writer.write("    pop " + calls[3] + "\n");
                        writer.write("    pop " + calls[2] + "\n");
                        writer.write("    pop " + calls[1] + "\n");
                        writer.write("    " + syscallKeyword + "\n");
                        writer.write("    push " + calls[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.SYSCALL4) {
                        writer.write("    pop " + calls[4] + "\n");
                        writer.write("    pop " + calls[3] + "\n");
                        writer.write("    pop " + calls[2] + "\n");
                        writer.write("    pop " + calls[1] + "\n");
                        writer.write("    " + syscallKeyword + "\n");
                        writer.write("    push " + calls[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.SYSCALL5) {
                        writer.write("    pop " + calls[5] + "\n");
                        writer.write("    pop " + calls[4] + "\n");
                        writer.write("    pop " + calls[3] + "\n");
                        writer.write("    pop " + calls[2] + "\n");
                        writer.write("    pop " + calls[1] + "\n");
                        writer.write("    " + syscallKeyword + "\n");
                        writer.write("    push " + calls[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.SYSCALL6) {
                        writer.write("    pop " + calls[6] + "\n");
                        writer.write("    pop " + calls[5] + "\n");
                        writer.write("    pop " + calls[4] + "\n");
                        writer.write("    pop " + calls[3] + "\n");
                        writer.write("    pop " + calls[2] + "\n");
                        writer.write("    pop " + calls[1] + "\n");
                        writer.write("    " + syscallKeyword + "\n");
                        writer.write("    push " + calls[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.SYSCALL7) {
                        writer.write("    pop " + calls[7] + "\n");
                        writer.write("    pop " + calls[6] + "\n");
                        writer.write("    pop " + calls[5] + "\n");
                        writer.write("    pop " + calls[4] + "\n");
                        writer.write("    pop " + calls[3] + "\n");
                        writer.write("    pop " + calls[2] + "\n");
                        writer.write("    pop " + calls[1] + "\n");
                        writer.write("    " + syscallKeyword + "\n");
                        writer.write("    push " + calls[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.PUSHSTR) {
                        writer.write("    push " + funcName + ".str" + instr.data[0] + "\n");
                    }else if(instr.instruction == BytecodeInstructions.POP8) {
                        writer.write("    inc " + stackPointer + "\n");
                    }else if(instr.instruction == BytecodeInstructions.POP16) {
                        writer.write("    add " + stackPointer + ", 2\n");
                    }else if(instr.instruction == BytecodeInstructions.POP32) {
                        writer.write("    add " + stackPointer + ", 4\n");
                    }else if(instr.instruction == BytecodeInstructions.POP64) {
                        writer.write("    add " + stackPointer + ", 8\n");
                    }else if(instr.instruction == BytecodeInstructions.RETURN) {
                        if(func.name.equals("_start")) {
                            writer.write("    mov " + calls[1] + ", " + syscallExit + "\n");
                            writer.write("    mov " + calls[2] + ", 0\n");
                            writer.write("    " + syscallKeyword + "\n");
                        }else {
                            writer.write("    leave\n");
                            writer.write("    ret\n");
                        }
                        runRet = false;
                        break;
                    }else if(instr.instruction == BytecodeInstructions.RETURN8) {
                        if(func.name.equals("_start")) {
                            writer.write("    mov byte dil, [" + stackPointer + "]\n");
                            writer.write("    mov " + calls[1] + ", " + syscallExit + "\n");
                            writer.write("    " + syscallKeyword + "\n");
                        }else {
                            writer.write("    mov byte al, [" + stackPointer + "]\n");
                            writer.write("    inc " + stackPointer + "\n");
                            writer.write("    leave\n");
                            writer.write("    ret\n");
                        }
                        runRet = false;
                        break;
                    }else if(instr.instruction == BytecodeInstructions.RETURN16) {
                        if(func.name.equals("_start")) {
                            writer.write("    mov word di, [" + stackPointer + "]\n");
                            writer.write("    mov " + calls[1] + ", " + syscallExit + "\n");
                            writer.write("    " + syscallKeyword + "\n");
                        }else {
                            writer.write("    mov word ax, [" + stackPointer + "]\n");
                            writer.write("    add " + stackPointer + ", 2\n");
                            writer.write("    leave\n");
                            writer.write("    ret\n");
                        }
                        runRet = false;
                        break;
                    }else if(instr.instruction == BytecodeInstructions.RETURN32) {
                        if(func.name.equals("_start")) {
                            writer.write("    mov dword edi, [" + stackPointer + "]\n");
                            writer.write("    mov " + calls[1] + ", " + syscallExit + "\n");
                            writer.write("    " + syscallKeyword + "\n");
                        }else {
                            writer.write("    mov dword eax, [" + stackPointer + "]\n");
                            writer.write("    add " + stackPointer + ", 4\n");
                            writer.write("    leave\n");
                            writer.write("    ret\n");
                        }
                        runRet = false;
                        break;
                    }else if(instr.instruction == BytecodeInstructions.RETURN64) {
                        if(func.name.equals("_start")) {
                            writer.write("    mov qword rdi, [" + stackPointer + "]\n");
                            writer.write("    mov " + calls[1] + ", " + syscallExit + "\n");
                            writer.write("    " + syscallKeyword + "\n");
                        }else {
                            writer.write("    mov qword rax, [" + stackPointer + "]\n");
                            writer.write("    add " + stackPointer + ", 8\n");
                            writer.write("    leave\n");
                            writer.write("    ret\n");
                        }
                        runRet = false;
                        break;
                    }else {
                        System.err.println("Missing instruction: " + instr);
                    }
                }
                if(runRet) {
                    if(func.name.equals("_start")) {
                        writer.write("    ; exit\n");
                        writer.write("    mov " + calls[1] + ", " + syscallExit + "\n");
                        writer.write("    mov " + calls[2] + ", 0\n");
                        writer.write("    " + syscallKeyword + "\n");
                    } else {
                        writer.write("    ; return\n");
                        writer.write("    leave\n");
                        writer.write("    ret\n");
                    }
                }

                ArrayList<String> strings = bc.getStrings();
                for(int i = 0; i<strings.size(); i++) {
                    char[] chars = strings.get(i).toCharArray();
                    String[] hexs = new String[chars.length];
                    for(int j = 0; j<chars.length; j++) {
                        hexs[j] = "0x" + Integer.toHexString(chars[j]);
                    }
                    writer.write(funcName + ".str" + i + ": db " + String.join(", ", hexs) + "\n");
                }
            }
        }

//        writer.write();

        writer.close();
    }

}
