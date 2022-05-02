package ga.epicpix.zprol.generators;

import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.StaticImports;
import ga.epicpix.zprol.compiled.Function;
import ga.epicpix.zprol.compiled.FunctionModifiers;
import ga.epicpix.zprol.compiled.FunctionSignature;
import ga.epicpix.zprol.compiled.GeneratedData;
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
        instructionGenerators.put("vreturn", (i, s) -> "  ret\n");
        instructionGenerators.put("breturn", (i, s) -> "  pop ax\n  ret\n");
        instructionGenerators.put("lreturn", (i, s) -> "  pop rax\n  ret\n");
        instructionGenerators.put("bpush", (i, s) -> "  push word " + i.getData()[0] + "\n");
        instructionGenerators.put("lpush", (i, s) -> {
            long v = ((Number) i.getData()[0]).longValue();
            if(v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
                return "  mov rax, " + v + "\n  push rax\n";
            }
            return "  push " + v + "\n";
        });
        instructionGenerators.put("badd", (i, s) -> "  pop cx\n  pop dx\n  add cx, dx\n  push cx\n");
        instructionGenerators.put("ladd", (i, s) -> "  pop rcx\n  pop rdx\n  add rcx, rdx\n  push rcx\n");
        instructionGenerators.put("bsub", (i, s) -> "  pop cx\n  pop dx\n  sub cx, dx\n  push cx\n");
        instructionGenerators.put("bmul", (i, s) -> "  pop cx\n  pop dx\n  imul cx, dx\n  push cx\n");
        instructionGenerators.put("lmul", (i, s) -> "  pop rcx\n  pop rdx\n  imul rcx, rdx\n  push rcx\n");
        instructionGenerators.put("lpop", (i, s) -> "  sub rsp, 8\n");
        instructionGenerators.put("invoke", (i, s) -> {
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
        public String generateInstruction(IBytecodeInstruction i, SeekIterator<IBytecodeInstruction> s);
    }

    public void generate(DataOutputStream outStream, GeneratedData generated) throws IOException {
        outStream.writeBytes("global _entry\n");
        outStream.writeBytes("_entry:\n");
        outStream.writeBytes("  call " + getMangledName(null, "main", new FunctionSignature(Language.getTypeFromDescriptor("V"))) + "\n");
        outStream.writeBytes("  mov rax, 60\n");
        outStream.writeBytes("  mov rdi, 0\n");
        outStream.writeBytes("  syscall\n");
        for(var function : generated.functions) {
            if(FunctionModifiers.isEmptyCode(function.modifiers())) continue;

            String functionName = getMangledName(function.namespace(), function.name(), function.signature());
            outStream.writeBytes(functionName + ":\n");
            System.out.println(" - " + functionName);
            System.out.println(function.code().getInstructions().stream().map(Object::toString).collect(Collectors.joining("\n")));
            SeekIterator<IBytecodeInstruction> instructions = new SeekIterator<>(function.code().getInstructions());
            while(instructions.hasNext()) {
                var instruction = instructions.next();
                var generator = instructionGenerators.get(instruction.getName());
                if(generator != null) {
                    outStream.writeBytes(generator.generateInstruction(instruction, instructions));
                }else {
                    throw new UndefinedOperationException("Unable to generate instructions for the " + instruction.getName() + " instruction");
                }
            }
        }
    }

    public static String getMangledName(String namespace, String name, FunctionSignature signature) {
        return (namespace != null ? namespace.replace('.', '$') + "$" : "") + name + "$" + signature.toString().replace("(", "").replace(")", "");
    }

}
