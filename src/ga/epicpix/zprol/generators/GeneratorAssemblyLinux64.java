package ga.epicpix.zprol.generators;

import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.compiled.*;
import ga.epicpix.zprol.compiled.bytecode.IBytecodeInstruction;
import ga.epicpix.zprol.exceptions.FunctionNotDefinedException;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.exceptions.UndefinedOperationException;
import ga.epicpix.zprol.zld.Language;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import static ga.epicpix.zprol.StaticImports.getInstructionPrefix;

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
    private static final String[] CALL_REGISTERS_16 = new String[] {"dx", "sx", "dx", "cx", "r8w", "r9w"};
    private static final String[] SYSCALL_REGISTERS_64 = new String[] {"rax", "rdi", "rsi", "rdx", "r10", "r8", "r9"};
    private static final String[] SYSCALL_REGISTERS_16 = new String[] {"ax", "di", "sx", "dx", "r10w", "r8w", "r9w"};

    static {
        instructionGenerators.put("vreturn", (i, s, f, lp) -> f.code().getLocalsSize() != 0 ? "  mov rsp, rbp\n  pop rbp\n  ret\n" : "  ret\n");
        instructionGenerators.put("breturn", (i, s, f, lp) -> f.code().getLocalsSize() != 0 ? "  pop ax\n  mov rsp, rbp\n  pop rbp\n  ret\n" : "  pop ax\n ret\n");
        instructionGenerators.put("lreturn", (i, s, f, lp) -> f.code().getLocalsSize() != 0 ? "  pop rax\n  mov rsp, rbp\n  pop rbp\n  ret\n" : "  pop rax\n  ret\n");
        instructionGenerators.put("bpush", (i, s, f, lp) -> "  push word " + i.getData()[0] + "\n");
        instructionGenerators.put("lpush", (i, s, f, lp) -> {
            long v = ((Number) i.getData()[0]).longValue();
            if(v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
                return "  mov rax, " + v + "\n  push rax\n";
            }
            return "  push " + v + "\n";
        });
        instructionGenerators.put("lload_local", (i, s, f, lp) -> "  push qword [rbp-" + i.getData()[0] + "]\n");
        instructionGenerators.put("lstore_local", (i, s, f, lp) -> "  pop qword [rbp-" + i.getData()[0] + "]\n");
        instructionGenerators.put("push_string", (i, s, f, lp) -> "  push _string" + lp.getOrCreateStringIndex((String) i.getData()[0]) + "\n");
        instructionGenerators.put("badd", (i, s, f, lp) -> "  pop cx\n  pop dx\n  add cx, dx\n  push cx\n");
        instructionGenerators.put("ladd", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  add rcx, rdx\n  push rcx\n");
        instructionGenerators.put("bsub", (i, s, f, lp) -> "  pop cx\n  pop dx\n  sub cx, dx\n  push cx\n");
        instructionGenerators.put("lsub", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  sub rcx, rdx\n  push rcx\n");
        instructionGenerators.put("bmul", (i, s, f, lp) -> "  pop cx\n  pop dx\n  imul cx, dx\n  push cx\n");
        instructionGenerators.put("lmul", (i, s, f, lp) -> "  pop rcx\n  pop rdx\n  imul rcx, rdx\n  push rcx\n");
        instructionGenerators.put("lpop", (i, s, f, lp) -> "  sub rsp, 8\n");
        instructionGenerators.put("invoke", (i, s, func, lp) -> {
            StringBuilder args = new StringBuilder();
            Function f = (Function) i.getData()[0];
            boolean isSyscall = FunctionModifiers.isEmptyCode(f.modifiers()) && f.name().equals("syscall");

            int x = f.signature().parameters().length - 1;
            for(var param : f.signature().parameters()) {
                if(param.getSize() == 1 || param.getSize() == 2) {
                    args.append("  pop ").append(isSyscall ? SYSCALL_REGISTERS_16[x] : CALL_REGISTERS_16[x]).append("\n");
                }else if(param.getSize() == 4 || param.getSize() == 8) {
                    args.append("  pop ").append(isSyscall ? SYSCALL_REGISTERS_64[x] : CALL_REGISTERS_64[x]).append("\n");
                }else {
                    throw new NotImplementedException("Not implemented size " + param.getSize());
                }
                x--;
            }
            if(FunctionModifiers.isEmptyCode(f.modifiers())) {
                if(isSyscall) {
                    args.append("  syscall\n");
                }else {
                    throw new FunctionNotDefinedException("Unknown function " + f.name());
                }
            }else {
                args.append("  call ").append(getMangledName(f.namespace(), f.name(), f.signature())).append("\n");
            }
            if(s.hasNext() && s.seek().getName().equals(getInstructionPrefix(f.signature().returnType().getSize()) + "pop")) {
                s.next();
                return args.toString();
            }

            var ret = f.signature().returnType();
            if (ret.getSize() == 1 || ret.getSize() == 2) {
                args.append("  push ax\n");
            } else if (ret.getSize() == 4 || ret.getSize() == 8) {
                args.append("  push rax\n");
            } else {
                throw new NotImplementedException("Not implemented size " + ret.getSize());
            }
            return args.toString();
        });
    }

    private interface InstructionGenerator {
        public String generateInstruction(IBytecodeInstruction i, SeekIterator<IBytecodeInstruction> s, Function function, ConstantPool lp);
    }

    public void generate(DataOutputStream outStream, GeneratedData generated) throws IOException {
        outStream.writeBytes("global _entry\n");
        outStream.writeBytes("_entry:\n");
        outStream.writeBytes("  call " + getMangledName(null, "main", new FunctionSignature(Language.getTypeFromDescriptor("V"))) + "\n");
        outStream.writeBytes("  mov rax, 60\n");
        outStream.writeBytes("  mov rdi, 0\n");
        outStream.writeBytes("  syscall\n");
        ConstantPool localConstantPool = new ConstantPool();
        for(var function : generated.functions) {
            if(FunctionModifiers.isEmptyCode(function.modifiers())) continue;

            String functionName = getMangledName(function.namespace(), function.name(), function.signature());
            outStream.writeBytes(functionName + ":\n");
            if(function.code().getLocalsSize() != 0) {
                outStream.writeBytes("  push rbp\n");
                outStream.writeBytes("  mov rbp, rsp\n");
                outStream.writeBytes("  sub rsp, " + function.code().getLocalsSize() + "\n");
            }
            int localsIndex = 0;
            PrimitiveType[] parameters = function.signature().parameters();
            for (int i = parameters.length - 1; i >= 0; i--) {
                PrimitiveType param = parameters[i];
                localsIndex += param.getSize();
                if(param.getSize() == 1 || param.getSize() == 2) {
                    outStream.writeBytes("  mov [rbp-" + localsIndex + "], " + CALL_REGISTERS_16[i] + "\n");
                }else if(param.getSize() == 4 || param.getSize() == 8) {
                    outStream.writeBytes("  mov [rbp-" + localsIndex + "], " + CALL_REGISTERS_64[i] + "\n");
                }
            }
            SeekIterator<IBytecodeInstruction> instructions = new SeekIterator<>(function.code().getInstructions());
            while(instructions.hasNext()) {
                var instruction = instructions.next();
                var generator = instructionGenerators.get(instruction.getName());
                if(generator != null) {
                    outStream.writeBytes(generator.generateInstruction(instruction, instructions, function, localConstantPool));
                }else {
                    throw new UndefinedOperationException("Unable to generate instructions for the " + instruction.getName() + " instruction");
                }
            }
        }
        int index = 1;
        for(var constantPoolEntry : localConstantPool.entries) {
            if(constantPoolEntry instanceof ConstantPoolEntry.StringEntry str) {
                outStream.writeBytes("_string" + index + ": db " + Arrays.stream(str.getString().chars().toArray()).mapToObj(x -> "0x" + Integer.toHexString(x)).collect(Collectors.joining(", ")) + "\n");
            }
            index++;
        }
    }

    public static String getMangledName(String namespace, String name, FunctionSignature signature) {
        return (namespace != null ? namespace.replace('.', '$') + "$" : "") + name + "$" + signature.toString().replace("(", "").replace(")", "");
    }

}
