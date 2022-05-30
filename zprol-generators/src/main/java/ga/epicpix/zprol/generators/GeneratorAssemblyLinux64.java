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

    public record Instruction(String data) {}

    private static final String[] CALL_REGISTERS_64 = new String[] {"rdi", "rsi", "rdx", "rcx", "r8", "r9"};
    private static final String[] CALL_REGISTERS_16 = new String[] {"di", "sx", "dx", "cx", "r8w", "r9w"};
    private static final String[] SYSCALL_REGISTERS_64 = new String[] {"rax", "rdi", "rsi", "rdx", "r10", "r8", "r9"};
    private static final String[] SYSCALL_REGISTERS_16 = new String[] {"ax", "di", "sx", "dx", "r10w", "r8w", "r9w"};

    static {
        instructionGenerators.put("vreturn", (i, s, f, lp, instructions) -> (f.code().getLocalsSize() != 0 ? instructions.add("mov rsp, rbp").add("pop rbp") : instructions).add("ret"));
        instructionGenerators.put("breturn", (i, s, f, lp, instructions) -> {
            instructions.add("pop ax");
            (f.code().getLocalsSize() != 0 ? instructions.add("mov rsp, rbp").add("pop rbp") : instructions).add("ret");
        });
        instructionGenerators.put("sreturn", (i, s, f, lp, instructions) -> {
            instructions.add("pop ax");
            (f.code().getLocalsSize() != 0 ? instructions.add("mov rsp, rbp").add("pop rbp") : instructions).add("ret");
        });
        instructionGenerators.put("ireturn", (i, s, f, lp, instructions) -> {
            instructions.add("pop rax");
            (f.code().getLocalsSize() != 0 ? instructions.add("mov rsp, rbp").add("pop rbp") : instructions).add("ret");
        });
        instructionGenerators.put("lreturn", (i, s, f, lp, instructions) -> {
            instructions.add("pop rax");
            (f.code().getLocalsSize() != 0 ? instructions.add("mov rsp, rbp").add("pop rbp") : instructions).add("ret");
        });
        instructionGenerators.put("areturn", (i, s, f, lp, instructions) -> {
            instructions.add("pop rax");
            (f.code().getLocalsSize() != 0 ? instructions.add("mov rsp, rbp").add("pop rbp") : instructions).add("ret");
        });
        instructionGenerators.put("bpush", (i, s, f, lp, instructions) -> instructions.add("push word " + i.getData()[0]));
        instructionGenerators.put("spush", (i, s, f, lp, instructions) -> instructions.add("push word " + i.getData()[0]));
        instructionGenerators.put("ipush", (i, s, f, lp, instructions) -> instructions.add("push qword " + i.getData()[0]));
        instructionGenerators.put("lpush", (i, s, f, lp, instructions) -> {
            long v = ((Number) i.getData()[0]).longValue();
            if(v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
                instructions.add("mov rax, " + v);
                instructions.add("push rax");
            }
            instructions.add("push " + v);
        });
        instructionGenerators.put("bload_local", (i, s, f, lp, instructions) -> instructions.add("xor cx, cx").add("mov cl, [rbp-" + i.getData()[0] + "]").add("push cx"));
        instructionGenerators.put("sload_local", (i, s, f, lp, instructions) -> instructions.add("push word [rbp-" + i.getData()[0] + "]"));
        instructionGenerators.put("iload_local", (i, s, f, lp, instructions) -> instructions.add("xor rcx, rcx").add("mov ecx, [rbp-" + i.getData()[0] + "]").add("push rcx"));
        instructionGenerators.put("lload_local", (i, s, f, lp, instructions) -> instructions.add("push qword [rbp-" + i.getData()[0] + "]"));
        instructionGenerators.put("aload_local", (i, s, f, lp, instructions) -> instructions.add("push qword [rbp-" + i.getData()[0] + "]"));
        instructionGenerators.put("bstore_local", (i, s, f, lp, instructions) -> instructions.add("pop cx").add("mov [rbp-" + i.getData()[0] + "], cl"));
        instructionGenerators.put("sstore_local", (i, s, f, lp, instructions) -> instructions.add("pop word [rbp-" + i.getData()[0] + "]"));
        instructionGenerators.put("istore_local", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("mov qword [rbp-" + i.getData()[0] + "], ecx"));
        instructionGenerators.put("lstore_local", (i, s, f, lp, instructions) -> instructions.add("pop qword [rbp-" + i.getData()[0] + "]"));
        instructionGenerators.put("astore_local", (i, s, f, lp, instructions) -> instructions.add("pop qword [rbp-" + i.getData()[0] + "]"));
        instructionGenerators.put("push_string", (i, s, f, lp, instructions) -> instructions.add("push _string" + lp.getOrCreateStringIndex((String) i.getData()[0])));
        instructionGenerators.put("bload_array", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("mov cl, [rdx + rcx]").add("push cx"));
        instructionGenerators.put("sload_array", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("mov cx, [rdx + rcx * 2]").add("push cx"));
        instructionGenerators.put("iload_array", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("mov ecx, [rdx + rcx * 4]").add("push rcx"));
        instructionGenerators.put("lload_array", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("mov rcx, [rdx + rcx * 8]").add("push rcx"));
        instructionGenerators.put("aload_array", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("mov rcx, [rdx + rcx * 8]").add("push rcx"));
        instructionGenerators.put("bstore_array", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("pop ax").add("mov [rdx + rcx], al"));
        instructionGenerators.put("sstore_array", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("pop ax").add("mov [rdx + rcx * 2], ax"));
        instructionGenerators.put("istore_array", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("pop rax").add("mov [rdx + rcx * 4], eax"));
        instructionGenerators.put("lstore_array", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("pop rax").add("mov [rdx + rcx * 8], rax"));
        instructionGenerators.put("astore_array", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("pop rax").add("mov [rdx + rcx * 8], rax"));
        instructionGenerators.put("class_field_load", (i, s, f, lp, instructions) -> {
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

            instructions.add("pop rcx");
            if(size == 1) {
                instructions.add("mov dl, [rcx+" + offset + "]");
                instructions.add("push dx");
            } else if(size == 2) instructions.add("push word [rcx+" + offset + "]");
            else if(size == 4) {
                instructions.add("mov edx, [rcx+" + offset + "]");
                instructions.add("push rdx");
            }
            else if(size == 8) instructions.add("push qword [rcx+" + offset + "]");
            else throw new IllegalStateException("Unsupported size " + size);

        });
        instructionGenerators.put("class_field_store", (i, s, f, lp, instructions) -> {
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

            instructions.add("pop rcx");
            if(size == 1) {
                instructions.add("pop dx");
                instructions.add("mov dl, [rcx+" + offset + "]");
            }
            else if(size == 2) instructions.add("pop word [rcx+" + offset + "]");
            else if(size == 4) {
                instructions.add("pop rdx");
                instructions.add("mov edx, [rcx+" + offset + "]");
            }
            else if(size == 8) instructions.add("pop qword [rcx+" + offset + "]");
            else throw new IllegalStateException("Unsupported size " + size);

        });
        instructionGenerators.put("ineq", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("xor rax, rax").add("cmp ecx, edx").add("mov rcx, 1").add("cmovne rax, rcx").add("push rax"));
        instructionGenerators.put("lneq", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("xor rax, rax").add("cmp rcx, rdx").add("mov rcx, 1").add("cmovne rax, rcx").add("push rax"));
        instructionGenerators.put("ieq", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("xor rax, rax").add("cmp ecx, edx").add("mov rcx, 1").add("cmove rax, rcx").add("push rax"));
        instructionGenerators.put("leq", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("xor rax, rax").add("cmp rcx, rdx").add("mov rcx, 1").add("cmove rax, rcx").add("push rax"));
        instructionGenerators.put("neqjmp", (i, s, f, lp, instructions) -> instructions.add("pop rax").add("cmp rax, 0").add("je " + getMangledName(f) + "." + (s.currentIndex() - 1 + ((Number)i.getData()[0]).shortValue())));
        instructionGenerators.put("eqjmp", (i, s, f, lp, instructions) -> instructions.add("pop rax").add("cmp rax, 0").add("jne " + getMangledName(f) + "." + (s.currentIndex() - 1 + ((Number)i.getData()[0]).shortValue())));
        instructionGenerators.put("jmp", (i, s, f, lp, instructions) -> instructions.add("jmp " + getMangledName(f) + "." + (s.currentIndex() - 1 + ((Number)i.getData()[0]).shortValue())));
        instructionGenerators.put("badd", (i, s, f, lp, instructions) -> instructions.add("pop cx").add("pop dx").add("add cl, dl").add("push cx"));
        instructionGenerators.put("sadd", (i, s, f, lp, instructions) -> instructions.add("pop cx").add("pop dx").add("add cx, dx").add("push cx"));
        instructionGenerators.put("iadd", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("add ecx, edx").add("push rcx"));
        instructionGenerators.put("ladd", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("add rcx, rdx").add("push rcx"));
        instructionGenerators.put("bsub", (i, s, f, lp, instructions) -> instructions.add("pop cx").add("pop dx").add("sub dl, cl").add("push dx"));
        instructionGenerators.put("ssub", (i, s, f, lp, instructions) -> instructions.add("pop cx").add("pop dx").add("sub dx, cx").add("push dx"));
        instructionGenerators.put("isub", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("sub edx, ecx").add("push rdx"));
        instructionGenerators.put("lsub", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("sub rdx, rcx").add("push rdx"));
        instructionGenerators.put("band", (i, s, f, lp, instructions) -> instructions.add("pop cx").add("pop dx").add("and cl, dl").add("push cx"));
        instructionGenerators.put("sand", (i, s, f, lp, instructions) -> instructions.add("pop cx").add("pop dx").add("and cx, dx").add("push cx"));
        instructionGenerators.put("iand", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("and ecx, edx").add("push rcx"));
        instructionGenerators.put("land", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("and rcx, rdx").add("push rcx"));
        instructionGenerators.put("bshift_left", (i, s, f, lp, instructions) -> instructions.add("pop cx").add("pop dx").add("shl dl, cl").add("push dx"));
        instructionGenerators.put("sshift_left", (i, s, f, lp, instructions) -> instructions.add("pop cx").add("pop dx").add("shl dx, cl").add("push dx"));
        instructionGenerators.put("ishift_left", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("shl edx, cl").add("push rdx"));
        instructionGenerators.put("lshift_left", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("shl rdx, cl").add("push rdx"));
        instructionGenerators.put("bshift_right", (i, s, f, lp, instructions) -> instructions.add("pop cx").add("pop dx").add("shr dl, cl").add("push dx"));
        instructionGenerators.put("sshift_right", (i, s, f, lp, instructions) -> instructions.add("pop cx").add("pop dx").add("shr dx, cl").add("push dx"));
        instructionGenerators.put("ishift_right", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("shr edx, cl").add("push rdx"));
        instructionGenerators.put("lshift_right", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("shr rdx, cl").add("push rdx"));
        instructionGenerators.put("bmul", (i, s, f, lp, instructions) -> instructions.add("pop cx").add("pop dx").add("imul cx, dx").add("push cx"));
        instructionGenerators.put("lmul", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("imul rcx, rdx").add("push rcx"));
        instructionGenerators.put("lmulu", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rax").add("xor rdx, rdx").add("mul rcx").add("push rdx"));
        instructionGenerators.put("idiv", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rax").add("xor rdx, rdx").add("idiv ecx").add("push rax"));
        instructionGenerators.put("ldiv", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rax").add("xor rdx, rdx").add("idiv rcx").add("push rax"));
        instructionGenerators.put("idivu", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rax").add("xor rdx, rdx").add("div ecx").add("push rax"));
        instructionGenerators.put("ldivu", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rax").add("xor rdx, rdx").add("div rcx").add("push rax"));
        instructionGenerators.put("imod", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rax").add("xor rdx, rdx").add("idiv ecx").add("push rdx"));
        instructionGenerators.put("lmod", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rax").add("xor rdx, rdx").add("idiv rcx").add("push rdx"));
        instructionGenerators.put("imodu", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rax").add("xor rdx, rdx").add("div ecx").add("push rdx"));
        instructionGenerators.put("lmodu", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rax").add("xor rdx, rdx").add("div rcx").add("push rdx"));
        instructionGenerators.put("lor", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("pop rdx").add("or rcx, rdx").add("push rcx"));
        instructionGenerators.put("bpop", (i, s, f, lp, instructions) -> instructions.add("add rsp, 2"));
        instructionGenerators.put("spop", (i, s, f, lp, instructions) -> instructions.add("add rsp, 2"));
        instructionGenerators.put("ipop", (i, s, f, lp, instructions) -> instructions.add("add rsp, 8"));
        instructionGenerators.put("lpop", (i, s, f, lp, instructions) -> instructions.add("add rsp, 8"));
        instructionGenerators.put("apop", (i, s, f, lp, instructions) -> instructions.add("add rsp, 8"));
        instructionGenerators.put("bdup", (i, s, f, lp, instructions) -> instructions.add("pop cx").add("push cx").add("push cx"));
        instructionGenerators.put("sdup", (i, s, f, lp, instructions) -> instructions.add("pop cx").add("push cx").add("push cx"));
        instructionGenerators.put("idup", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("push rcx").add("push rcx"));
        instructionGenerators.put("ldup", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("push rcx").add("push rcx"));
        instructionGenerators.put("adup", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("push rcx").add("push rcx"));
        instructionGenerators.put("bcasti", (i, s, f, lp, instructions) -> instructions.add("pop cx").add("movsx ecx, cx").add("push rcx"));
        instructionGenerators.put("bcastl", (i, s, f, lp, instructions) -> instructions.add("pop cx").add("movsx rcx, cx").add("push rcx"));
        instructionGenerators.put("icastl", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("movsxd rcx, ecx").add("push rcx"));
        instructionGenerators.put("lcasts", (i, s, f, lp, instructions) -> instructions.add("pop rcx").add("push cx"));
        instructionGenerators.put("invoke", (i, s, func, lp, instructions) -> invokeMethod((Function) i.getData()[0], instructions));
    }

    private static void invokeMethod(Function f, InstructionList instructions) {
        boolean isSyscall = FunctionModifiers.isEmptyCode(f.modifiers()) && f.name().equals("syscall");

        var params = f.signature().parameters();
        for(int x = params.length - 1; x>=0; x--) {
            var param = params[x];
            if(param instanceof PrimitiveType primitive) {
                if (primitive.getSize() == 1 || primitive.getSize() == 2) {
                    instructions.add("pop " + (isSyscall ? SYSCALL_REGISTERS_16[x] : CALL_REGISTERS_16[x]));
                } else if (primitive.getSize() == 4 || primitive.getSize() == 8) {
                    instructions.add("pop " + (isSyscall ? SYSCALL_REGISTERS_64[x] : CALL_REGISTERS_64[x]));
                } else {
                    throw new NotImplementedException("Not implemented size " + primitive.getSize());
                }
            }else {
                instructions.add("pop " + (isSyscall ? SYSCALL_REGISTERS_64[x] : CALL_REGISTERS_64[x]));
            }
        }

        if (FunctionModifiers.isEmptyCode(f.modifiers())) {
            if (isSyscall) {
                instructions.add("syscall");
            } else {
                throw new FunctionNotDefinedException("Unknown native function " + f.name());
            }
        } else {
            instructions.add("call " + getMangledName(f));
        }

        var ret = f.signature().returnType();
        if(ret instanceof PrimitiveType primitive) {
            if (primitive.getSize() == 0) {
            } else if (primitive.getSize() == 1 || primitive.getSize() == 2) {
                instructions.add("push ax");
            } else if (primitive.getSize() == 4 || primitive.getSize() == 8) {
                instructions.add("push rax");
            } else {
                throw new NotImplementedException("Not implemented size " + primitive.getSize());
            }
        }else if(ret instanceof ClassType) {
            instructions.add("push rax");
        }else if(ret instanceof BooleanType) {
            instructions.add("push rax");
        }else if(ret instanceof ArrayType) {
            instructions.add("push rax");
        }else if(!(ret instanceof VoidType)) {
            throw new IllegalStateException("Cannot finish generating call instruction because of an unknown type '" + ret.getName() + "'");
        }
    }

    private static void writeFunction(Function function, ConstantPool localConstantPool, InstructionList assembly) {
        if(function.code().getLocalsSize() != 0) {
            assembly.add("push rbp");
            assembly.add("mov rbp, rsp");
            assembly.add("sub rsp, " + function.code().getLocalsSize());
        }
        int localsIndex = 0;
        var parameters = function.signature().parameters();
        for (int i = 0; i < parameters.length; i++) {
            var param = parameters[i];
            if(param instanceof PrimitiveType primitive) {
                if (primitive.getSize() == 1 || primitive.getSize() == 2) {
                    localsIndex += 2;
                    assembly.add("mov [rbp-" + localsIndex + "], " + CALL_REGISTERS_16[i]);
                } else if (primitive.getSize() == 4 || primitive.getSize() == 8) {
                    localsIndex += 8;
                    assembly.add("mov [rbp-" + localsIndex + "], " + CALL_REGISTERS_64[i]);
                } else {
                    localsIndex += primitive.getSize();
                }
            }else {
                localsIndex += 8;
                assembly.add("mov [rbp-" + localsIndex + "], " + CALL_REGISTERS_64[i]);
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
            if(labelList.contains(instructions.currentIndex())) {
                assembly.add(new Instruction(getMangledName(function) + "." + instructions.currentIndex() + ":"));
            }
            var instruction = instructions.next();
            assembly.add(new Instruction("; " + instruction.getName()));
            var generator = instructionGenerators.get(instruction.getName());
            if(generator != null) {
                generator.generateInstruction(instruction, instructions, function, localConstantPool, assembly);
            }else {
                throw new UnknownInstructionException("Unable to generate instructions for the " + instruction.getName() + " instruction");
            }
        }
    }

    private static class InstructionList {

        public final ArrayList<Instruction> instructions = new ArrayList<>();

        public InstructionList add(Instruction instr) {
            instructions.add(instr);
            return this;
        }

        public InstructionList add(String instr) {
            return add(new Instruction(instr));
        }

    }

    private interface InstructionGenerator {
        public void generateInstruction(IBytecodeInstruction i, SeekIterator<IBytecodeInstruction> s, Function function, ConstantPool lp, InstructionList instructions);
    }

    public void generate(DataOutputStream outStream, GeneratedData generated) throws IOException {
        InstructionList assembly = new InstructionList();

        assembly.add("global _entry");
        assembly.add("_entry:");
        assembly.add("call " + getMangledName(null, "main", new FunctionSignature(Types.getTypeFromDescriptor("V"))));
        assembly.add("mov rax, 60");
        assembly.add("mov rdi, 0");
        assembly.add("syscall");
        ConstantPool localConstantPool = new ConstantPool();
        for(var function : generated.functions) {
            if(FunctionModifiers.isEmptyCode(function.modifiers())) continue;

            assembly.add(getMangledName(function) + ":");
            writeFunction(function, localConstantPool, assembly);
        }
        int index = 1;
        for(var constantPoolEntry : localConstantPool.entries) {
            if(constantPoolEntry instanceof ConstantPoolEntry.StringEntry str) {
                assembly.add("_string" + index + ".chars" + ": db " + Arrays.stream(str.getString().chars().toArray()).mapToObj(x -> "0x" + Integer.toHexString(x)).collect(Collectors.joining(", ")));
                assembly.add("_string" + index + ":");
                assembly.add("dq " + str.getString().length());
                assembly.add("dq _string" + index + ".chars");
            }
            index++;
        }
        outStream.writeBytes(assembly.instructions.stream().map(Instruction::data).collect(Collectors.joining("\n")));
    }

    public static String getMangledName(Function func) {
        return getMangledName(func.namespace(), func.name(), func.signature());
    }

    public static String getMangledName(String namespace, String name, FunctionSignature signature) {
        return (namespace != null ? namespace.replace('.', '$') + "$" : "") + name + "$" + signature.toString().replace("(", "").replace(")", "").replace(";", "").replace("[", "r");
    }

}
