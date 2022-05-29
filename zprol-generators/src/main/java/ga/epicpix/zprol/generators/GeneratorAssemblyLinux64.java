package ga.epicpix.zprol.generators;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zpil.exceptions.FunctionNotDefinedException;
import ga.epicpix.zpil.exceptions.UnknownInstructionException;
import ga.epicpix.zpil.pool.ConstantPool;
import ga.epicpix.zpil.pool.ConstantPoolEntry;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.structures.*;
import ga.epicpix.zprol.structures.Class;
import ga.epicpix.zprol.types.*;
import ga.epicpix.zprol.utils.SeekIterator;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public final class GeneratorAssemblyLinux64 extends Generator {

    public String getGeneratorCommandLine() {
        return "asmlinux64";
    }

    public String getGeneratorName() {
        return "Assembly x86-64 Linux";
    }

    public String getFileExtension() {
        return ".asm";
    }

    private static final HashMap<String, InstructionGenerator> instructionGenerators = new HashMap<>();

    private static final String[] CALL_REGISTERS_64 = new String[] {"rdi", "rsi", "rdx", "rcx", "r8", "r9"};
    private static final String[] CALL_REGISTERS_16 = new String[] {"di", "sx", "dx", "cx", "r8w", "r9w"};
    private static final String[] SYSCALL_REGISTERS_64 = new String[] {"rax", "rdi", "rsi", "rdx", "r10", "r8", "r9"};
    private static final String[] SYSCALL_REGISTERS_16 = new String[] {"ax", "di", "sx", "dx", "r10w", "r8w", "r9w"};

    static {
        instructionGenerators.put("vreturn", (i, s, f, lp) -> (f.code().getLocalsSize() != 0 ? "  mov rsp, rbp\n  pop rbp\n" : "") + "  ret\n");
        instructionGenerators.put("breturn", (i, s, f, lp) -> (f.code().getLocalsSize() != 0 ? "  pop ax\n  mov rsp, rbp\n  pop rbp\n" : "  pop ax\n") + "  ret\n");
        instructionGenerators.put("lreturn", (i, s, f, lp) -> (f.code().getLocalsSize() != 0 ? "  pop rax\n  mov rsp, rbp\n  pop rbp\n" : "  pop rax\n") + "  ret\n");
        instructionGenerators.put("areturn", (i, s, f, lp) -> (f.code().getLocalsSize() != 0 ? "  pop rax\n  mov rsp, rbp\n  pop rbp\n" : "  pop rax\n") + "  ret\n");
        instructionGenerators.put("bpush", (i, s, f, lp) -> "  push word " + i.getData()[0] + "\n");
        instructionGenerators.put("spush", (i, s, f, lp) -> "  push word " + i.getData()[0] + "\n");
        instructionGenerators.put("ipush", (i, s, f, lp) -> "  push qword " + i.getData()[0] + "\n");
        instructionGenerators.put("lpush", (i, s, f, lp) -> {
            long v = ((Number) i.getData()[0]).longValue();
            if(v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
                return "  mov rax, " + v + "\n  push rax\n";
            }
            return "  push " + v + "\n";
        });
        instructionGenerators.put("bload_local", (i, s, f, lp) -> "  push word [rbp-" + i.getData()[0] + "]\n");
        instructionGenerators.put("iload_local", (i, s, f, lp) -> "  push qword [rbp-" + i.getData()[0] + "]\n");
        instructionGenerators.put("lload_local", (i, s, f, lp) -> "  push qword [rbp-" + i.getData()[0] + "]\n");
        instructionGenerators.put("aload_local", (i, s, f, lp) -> "  push qword [rbp-" + i.getData()[0] + "]\n");
        instructionGenerators.put("bstore_local", (i, s, f, lp) -> "  pop word [rbp-" + i.getData()[0] + "]\n");
        instructionGenerators.put("istore_local", (i, s, f, lp) -> "  pop qword [rbp-" + i.getData()[0] + "]\n");
        instructionGenerators.put("lstore_local", (i, s, f, lp) -> "  pop qword [rbp-" + i.getData()[0] + "]\n");
        instructionGenerators.put("astore_local", (i, s, f, lp) -> "  pop qword [rbp-" + i.getData()[0] + "]\n");
        instructionGenerators.put("push_string", (i, s, f, lp) -> "  push _string" + lp.getOrCreateStringIndex((String) i.getData()[0]) + "\n");
        instructionGenerators.put("bload_array", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  mov byte cl, [rdx + rcx]\n  push cx\n");
        instructionGenerators.put("sload_array", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  mov word cx, [rdx + rcx * 2]\n  push cx\n");
        instructionGenerators.put("iload_array", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  mov word ecx, [rdx + rcx * 4]\n  push rcx\n");
        instructionGenerators.put("lload_array", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  mov qword rcx, [rdx + rcx * 8]\n  push rcx\n");
        instructionGenerators.put("aload_array", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  mov qword rcx, [rdx + rcx * 8]\n  push rcx\n");
        instructionGenerators.put("bstore_array", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  pop ax\n  mov byte [rdx + rcx], al\n");
        instructionGenerators.put("sstore_array", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  pop ax\n  mov word [rdx + rcx * 2], ax\n");
        instructionGenerators.put("istore_array", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  pop rax\n  mov word [rdx + rcx * 4], eax\n");
        instructionGenerators.put("lstore_array", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  pop rax\n  mov qword [rdx + rcx * 8], rax\n");
        instructionGenerators.put("astore_array", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  pop rax\n  mov qword [rdx + rcx * 8], rax\n");
        instructionGenerators.put("class_field_load", (i, s, f, lp) -> {
            var clz = (Class) i.getData()[0];
            var fieldName = (String) i.getData()[1];
            ClassField field = null;
            int offset = 0;
            for(var e : clz.fields()) {
                if(e.name().equals(fieldName)) {
                    field = e;
                    break;
                }
                if(e.type() instanceof PrimitiveType t) offset += t.getSize();
                else if(e.type() instanceof BooleanType) offset += 8;
                else if(e.type() instanceof ArrayType) offset += 8;
                else if(e.type() instanceof ClassType) offset += 8;
                else throw new IllegalStateException("Cannot get size of type '" + e.type().getName() + "'");
            }
            if(field == null) {
                throw new IllegalStateException("Field '" + fieldName + "' not found in class '" + (clz.namespace() != null ? clz.namespace() + "." : "") + clz.name() + "'");
            }

            int size = 0;
            if(field.type() instanceof ClassType) size = 8;
            else if(field.type() instanceof BooleanType) size = 8;
            else if(field.type() instanceof ArrayType) size = 8;
            else if(field.type() instanceof PrimitiveType primitive) size = primitive.getSize();
            else throw new IllegalStateException("Cannot get size of type '" + field.type().getName() + "'");

            if(size == 1) return "  pop rcx\n  mov dl, [rcx+" + offset + "]\n  push dx\n";
            else if(size == 2) return "  pop rcx\n  push word [rcx+" + offset + "]\n";
            else if(size == 4) return "  pop rcx\n  mov edx, [rcx+" + offset + "]\n  push rdx\n";
            else if(size == 8) return "  pop rcx\n  push qword [rcx+" + offset + "]\n";
            else throw new IllegalStateException("Unsupported size " + size);

        });
        instructionGenerators.put("class_field_store", (i, s, f, lp) -> {
            var clz = (Class) i.getData()[0];
            var fieldName = (String) i.getData()[1];
            ClassField field = null;
            int offset = 0;
            for(var e : clz.fields()) {
                if(e.name().equals(fieldName)) {
                    field = e;
                    break;
                }
                if(e.type() instanceof PrimitiveType t) offset += t.getSize();
                else if(e.type() instanceof BooleanType) offset += 8;
                else if(e.type() instanceof ArrayType) offset += 8;
                else if(e.type() instanceof ClassType) offset += 8;
                else throw new IllegalStateException("Cannot get size of type '" + e.type().getName() + "'");
            }
            if(field == null) {
                throw new IllegalStateException("Field '" + fieldName + "' not found in class '" + (clz.namespace() != null ? clz.namespace() + "." : "") + clz.name() + "'");
            }

            int size = 0;
            if(field.type() instanceof ClassType) size = 8;
            else if(field.type() instanceof BooleanType) size = 8;
            else if(field.type() instanceof ArrayType) size = 8;
            else if(field.type() instanceof PrimitiveType primitive) size = primitive.getSize();
            else throw new IllegalStateException("Cannot get size of type '" + field.type().getName() + "'");

            if(size == 1) return "  pop rcx\n  pop dx\n  mov dl, [rcx+" + offset + "]\n";
            else if(size == 2) return "  pop rcx\n  pop word [rcx+" + offset + "]\n";
            else if(size == 4) return "  pop rcx\n  pop rdx\n  mov edx, [rcx+" + offset + "]\n";
            else if(size == 8) return "  pop rcx\n  pop qword [rcx+" + offset + "]\n";
            else throw new IllegalStateException("Unsupported size " + size);

        });
        instructionGenerators.put("ineq", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  xor rax, rax\n  cmp ecx, edx\n  mov rcx, 1\n  cmovne rax, rcx\n  push rax\n");
        instructionGenerators.put("lneq", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  xor rax, rax\n  cmp rcx, rdx\n  mov rcx, 1\n  cmovne rax, rcx\n  push rax\n");
        instructionGenerators.put("ieq", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  xor rax, rax\n  cmp ecx, edx\n  mov rcx, 1\n  cmove rax, rcx\n  push rax\n");
        instructionGenerators.put("leq", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  xor rax, rax\n  cmp rcx, rdx\n  mov rcx, 1\n  cmove rax, rcx\n  push rax\n");
        instructionGenerators.put("neqjmp", (i, s, f, lp) -> "  pop rax\n  cmp rax, 0\n  je " + (getMangledName(f.namespace(), f.name(), f.signature()) + "." + (s.currentIndex() - 1 + ((Number)i.getData()[0]).shortValue())) + "\n");
        instructionGenerators.put("eqjmp", (i, s, f, lp) -> "  pop rax\n  cmp rax, 0\n  jne " + (getMangledName(f.namespace(), f.name(), f.signature()) + "." + (s.currentIndex() - 1 + ((Number)i.getData()[0]).shortValue())) + "\n");
        instructionGenerators.put("jmp", (i, s, f, lp) -> "  jmp " + (getMangledName(f.namespace(), f.name(), f.signature()) + "." + (s.currentIndex() - 1 + ((Number)i.getData()[0]).shortValue())) + "\n");
        instructionGenerators.put("badd", (i, s, f, lp) -> "  pop cx\n  pop dx\n  add cx, dx\n  push cx\n");
        instructionGenerators.put("iadd", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  add ecx, edx\n  push rcx\n");
        instructionGenerators.put("ladd", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  add rcx, rdx\n  push rcx\n");
        instructionGenerators.put("bsub", (i, s, f, lp) -> "  pop cx\n  pop dx\n  sub cx, dx\n  push cx\n");
        instructionGenerators.put("lsub", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  sub rcx, rdx\n  push rcx\n");
        instructionGenerators.put("band", (i, s, f, lp) -> "  pop cx\n  pop dx\n  and cl, ch\n  push cx\n");
        instructionGenerators.put("sand", (i, s, f, lp) -> "  pop cx\n  pop dx\n  and cx, cx\n  push cx\n");
        instructionGenerators.put("iand", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  and ecx, edx\n  push rcx\n");
        instructionGenerators.put("land", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  and rcx, rdx\n  push rcx\n");
        instructionGenerators.put("bshift_left", (i, s, f, lp) -> "  pop cx\n  pop dx\n  shl dl, cl\n  push dx\n");
        instructionGenerators.put("sshift_left", (i, s, f, lp) -> "  pop cx\n  pop dx\n  shl dx, cl\n  push dx\n");
        instructionGenerators.put("ishift_left", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  shl edx, cl\n  push rdx\n");
        instructionGenerators.put("lshift_left", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  shl rdx, cl\n  push rdx\n");
        instructionGenerators.put("bshift_right", (i, s, f, lp) -> "  pop cx\n  pop dx\n  shr dl, cl\n  push dx\n");
        instructionGenerators.put("sshift_right", (i, s, f, lp) -> "  pop cx\n  pop dx\n  shl dx, cl\n  push dx\n");
        instructionGenerators.put("ishift_right", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  shr edx, cl\n  push rdx\n");
        instructionGenerators.put("lshift_right", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  shr rdx, cl\n  push rdx\n");
        instructionGenerators.put("bmul", (i, s, f, lp) -> "  pop cx\n  pop dx\n  imul cx, dx\n  push cx\n");
        instructionGenerators.put("lmul", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  imul rcx, rdx\n  push rcx\n");
        instructionGenerators.put("idiv", (i, s, f, lp) -> "  pop rcx\n  pop rax\n  xor rdx, rdx\n  idiv ecx\n  push rax\n");
        instructionGenerators.put("ldiv", (i, s, f, lp) -> "  pop rcx\n  pop rax\n  xor rdx, rdx\n  idiv rcx\n  push rax\n");
        instructionGenerators.put("idivu", (i, s, f, lp) -> "  pop rcx\n  pop rax\n  xor rdx, rdx\n  div ecx\n  push rax\n");
        instructionGenerators.put("ldivu", (i, s, f, lp) -> "  pop rcx\n  pop rax\n  xor rdx, rdx\n  div rcx\n  push rax\n");
        instructionGenerators.put("imod", (i, s, f, lp) -> "  pop rcx\n  pop rax\n  xor rdx, rdx\n  idiv ecx\n  push rdx\n");
        instructionGenerators.put("lmod", (i, s, f, lp) -> "  pop rcx\n  pop rax\n  xor rdx, rdx\n  idiv rcx\n  push rdx\n");
        instructionGenerators.put("imodu", (i, s, f, lp) -> "  pop rcx\n  pop rax\n  xor rdx, rdx\n  div ecx\n  push rdx\n");
        instructionGenerators.put("lmodu", (i, s, f, lp) -> "  pop rcx\n  pop rax\n  xor rdx, rdx\n  div rcx\n  push rdx\n");
        instructionGenerators.put("lmulu", (i, s, f, lp) -> "  pop rcx\n  pop rax\n  mov rdx, 0\n  mul rcx\n  push rdx\n");
        instructionGenerators.put("lor", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  or rcx, rdx\n  push rcx\n");
        instructionGenerators.put("lpop", (i, s, f, lp) -> "  add rsp, 8\n");
        instructionGenerators.put("apop", (i, s, f, lp) -> "  add rsp, 8\n");
        instructionGenerators.put("ldup", (i, s, f, lp) -> "  pop rcx\n  push rcx\n  push rcx\n");
        instructionGenerators.put("bcasti", (i, s, f, lp) -> "  pop cx\n  movsx ecx, cx\n  push rcx\n");
        instructionGenerators.put("bcastl", (i, s, f, lp) -> "  pop cx\n  movsx rcx, cx\n  push rcx\n");
        instructionGenerators.put("icastl", (i, s, f, lp) -> "  pop rcx\n  movsxd rcx, ecx\n  push rcx\n");
        instructionGenerators.put("lcasts", (i, s, f, lp) -> "  pop rcx\n  push cx\n");
        instructionGenerators.put("invoke", (i, s, func, lp) -> invokeMethod((Function) i.getData()[0], lp));
    }

    private static String invokeMethod(Function f, ConstantPool localConstantPool) {
        StringBuilder args = new StringBuilder();
        boolean isSyscall = FunctionModifiers.isEmptyCode(f.modifiers()) && f.name().equals("syscall");

        var params = f.signature().parameters();
        for(int x = params.length - 1; x>=0; x--) {
            var param = params[x];
            if(param instanceof PrimitiveType primitive) {
                if (primitive.getSize() == 1 || primitive.getSize() == 2) {
                    args.append("  pop ").append(isSyscall ? SYSCALL_REGISTERS_16[x] : CALL_REGISTERS_16[x]).append("\n");
                } else if (primitive.getSize() == 4 || primitive.getSize() == 8) {
                    args.append("  pop ").append(isSyscall ? SYSCALL_REGISTERS_64[x] : CALL_REGISTERS_64[x]).append("\n");
                } else {
                    throw new NotImplementedException("Not implemented size " + primitive.getSize());
                }
            }else {
                args.append("  pop ").append(isSyscall ? SYSCALL_REGISTERS_64[x] : CALL_REGISTERS_64[x]).append("\n");
            }
        }

        if (FunctionModifiers.isEmptyCode(f.modifiers())) {
            if (isSyscall) {
                args.append("  syscall\n");
            } else {
                throw new FunctionNotDefinedException("Unknown native function " + f.name());
            }
        } else {
            args.append("  call ").append(getMangledName(f.namespace(), f.name(), f.signature())).append("\n");
        }

        var ret = f.signature().returnType();
        if(ret instanceof PrimitiveType primitive) {
            if (primitive.getSize() == 0) {
            } else if (primitive.getSize() == 1 || primitive.getSize() == 2) {
                args.append("  push ax\n");
            } else if (primitive.getSize() == 4 || primitive.getSize() == 8) {
                args.append("  push rax\n");
            } else {
                throw new NotImplementedException("Not implemented size " + primitive.getSize());
            }
        }else if(ret instanceof ClassType) {
            args.append("  push rax\n");
        }else if(ret instanceof BooleanType) {
            args.append("  push rax\n");
        }else if(ret instanceof ArrayType) {
            args.append("  push rax\n");
        }else if(!(ret instanceof VoidType)) {
            throw new IllegalStateException("Cannot finish generating call instruction because of an unknown type '" + ret.getName() + "'");
        }
        return args.toString();
    }

    private static String writeFunction(Function function, ConstantPool localConstantPool) {
        StringBuilder outStream = new StringBuilder();
        if(function.code().getLocalsSize() != 0) {
            outStream.append("  push rbp\n");
            outStream.append("  mov rbp, rsp\n");
            outStream.append("  sub rsp, ").append(function.code().getLocalsSize()).append("\n");
        }
        int localsIndex = 0;
        var parameters = function.signature().parameters();
        for (int i = 0; i < parameters.length; i++) {
            var param = parameters[i];
            if(param instanceof PrimitiveType primitive) {
                if (primitive.getSize() == 1 || primitive.getSize() == 2) {
                    localsIndex += 2;
                    outStream.append("  mov [rbp-").append(localsIndex).append("], ").append(CALL_REGISTERS_16[i]).append("\n");
                } else if (primitive.getSize() == 4 || primitive.getSize() == 8) {
                    localsIndex += 8;
                    outStream.append("  mov [rbp-").append(localsIndex).append("], ").append(CALL_REGISTERS_64[i]).append("\n");
                } else {
                    localsIndex += primitive.getSize();
                }
            }else {
                localsIndex += 8;
                outStream.append("  mov [rbp-").append(localsIndex).append("], ").append(CALL_REGISTERS_64[i]).append("\n");
            }
        }
        var labelList = new ArrayList<Integer>();
        int c = 0;
        for(var instr : function.code().getInstructions()) {
            if(instr.getName().equals("jmp") || instr.getName().equals("eqjmp") || instr.getName().equals("neqjmp")) {
                labelList.add(c + ((Number) instr.getData()[0]).shortValue());
            }
            c++;
        }
        SeekIterator<IBytecodeInstruction> instructions = new SeekIterator<>(function.code().getInstructions());
        while(instructions.hasNext()) {
            if(labelList.contains(instructions.currentIndex() - 1)) {
                outStream.append(getMangledName(function.namespace(), function.name(), function.signature())).append(".").append(instructions.currentIndex() - 1).append(":\n");
            }
            var instruction = instructions.next();
            outStream.append("; ").append(instruction.getName()).append("\n");
            var generator = instructionGenerators.get(instruction.getName());
            if(generator != null) {
                outStream.append(generator.generateInstruction(instruction, instructions, function, localConstantPool));
            }else {
                throw new UnknownInstructionException("Unable to generate instructions for the " + instruction.getName() + " instruction");
            }
        }
        return outStream.toString();
    }

    private interface InstructionGenerator {
        public String generateInstruction(IBytecodeInstruction i, SeekIterator<IBytecodeInstruction> s, Function function, ConstantPool lp);
    }

    public void generate(DataOutputStream outStream, GeneratedData generated) throws IOException {
        outStream.writeBytes("global _entry\n");
        outStream.writeBytes("_entry:\n");
        outStream.writeBytes("  call " + getMangledName(null, "main", new FunctionSignature(Types.getTypeFromDescriptor("V"))) + "\n");
        outStream.writeBytes("  mov rax, 60\n");
        outStream.writeBytes("  mov rdi, 0\n");
        outStream.writeBytes("  syscall\n");
        ConstantPool localConstantPool = new ConstantPool();
        for(var function : generated.functions) {
            if(FunctionModifiers.isEmptyCode(function.modifiers())) continue;

            String functionName = getMangledName(function.namespace(), function.name(), function.signature());
            outStream.writeBytes(functionName + ":\n");
            outStream.writeBytes(writeFunction(function, localConstantPool));
        }
        int index = 1;
        for(var constantPoolEntry : localConstantPool.entries) {
            if(constantPoolEntry instanceof ConstantPoolEntry.StringEntry str) {
                outStream.writeBytes("_string" + index + ".chars" + ": db " + Arrays.stream(str.getString().chars().toArray()).mapToObj(x -> "0x" + Integer.toHexString(x)).collect(Collectors.joining(", ")) + "\n");
                outStream.writeBytes("_string" + index + ":\n  dq " + str.getString().length() + "\n  dq _string" + index + ".chars\n");
            }
            index++;
        }
    }

    public static String getMangledName(String namespace, String name, FunctionSignature signature) {
        return (namespace != null ? namespace.replace('.', '$') + "$" : "") + name + "$" + signature.toString().replace("(", "").replace(")", "").replace(";", "").replace("[", "r");
    }

}
