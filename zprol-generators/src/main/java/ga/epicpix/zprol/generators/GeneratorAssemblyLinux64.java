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
import java.util.*;
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

    public static class Instruction {
        private final String data;

        public Instruction(String data) {
            this.data = data;
        }

        public String data() {
            return data;
        }
    }

    public static class PushInstruction extends Instruction {

        private final String register;

        public PushInstruction(String register) {
            super("push " + register);
            this.register = register;
        }

        public String register() {
            return register;
        }
    }

    public static class PushNumberInstruction extends Instruction {

        private final long number;

        public PushNumberInstruction(long number) {
            super("push " + number);
            this.number = number;
        }

        public long number() {
            return number;
        }
    }

    public static class PopInstruction extends Instruction {

        private final String register;

        public PopInstruction(String register) {
            super("pop " + register);
            this.register = register;
        }

        public String register() {
            return register;
        }
    }

    public static class CallInstruction extends Instruction {

        private final String location;
        private final Function function;

        public CallInstruction(String location, Function func) {
            super("call " + location);
            this.location = location;
            function = func;
        }

        public String location() {
            return location;
        }

        public Function function() {
            return function;
        }

    }

    public static class ReturnInstruction extends Instruction {

        public ReturnInstruction() {
            super("ret");
        }

    }

    public static class ValueReturnInstruction extends Instruction {

        private final long value;

        public ValueReturnInstruction(long value) {
            super("mov rax, " + value + "\nret");
            this.value = value;
        }

        public long value() {
            return value;
        }

    }

    public static class FunctionStartInstruction extends Instruction {

        private final Function function;

        public FunctionStartInstruction(Function func) {
            super(getMangledName(func) + ":");
            function = func;
        }

        public Function function() {
            return function;
        }

    }

    public static PushInstruction push(String reg) {
        return new PushInstruction(reg);
    }

    public static PushNumberInstruction push(long num) {
        return new PushNumberInstruction(num);
    }

    public static PopInstruction pop(String reg) {
        return new PopInstruction(reg);
    }

    public static CallInstruction call(String reg, Function func) {
        return new CallInstruction(reg, func);
    }

    public static ReturnInstruction ret() {
        return new ReturnInstruction();
    }

    private static final String[] CALL_REGISTERS_64 = new String[] {"rdi", "rsi", "rdx", "rcx", "r8", "r9"};
    private static final String[] CALL_REGISTERS_16 = new String[] {"di", "sx", "dx", "cx", "r8w", "r9w"};
    private static final String[] SYSCALL_REGISTERS_64 = new String[] {"rax", "rdi", "rsi", "rdx", "r10", "r8", "r9"};
    private static final String[] SYSCALL_REGISTERS_16 = new String[] {"ax", "di", "sx", "dx", "r10w", "r8w", "r9w"};

    static {
        instructionGenerators.put("vreturn", (st, i, s, f, lp, instructions) -> instructions.add("mov rsp, rbp", pop("rbp")).add(ret()));
        instructionGenerators.put("breturn", (st, i, s, f, lp, instructions) -> {
            instructions.add(pop("ax"));
            instructions.add("mov rsp, rbp", pop("rbp")).add(ret());
        });
        instructionGenerators.put("sreturn", (st, i, s, f, lp, instructions) -> {
            instructions.add(pop("ax"));
            instructions.add("mov rsp, rbp", pop("rbp")).add(ret());
        });
        instructionGenerators.put("ireturn", (st, i, s, f, lp, instructions) -> {
            instructions.add(pop("rax"));
            instructions.add("mov rsp, rbp", pop("rbp")).add(ret());
        });
        instructionGenerators.put("lreturn", (st, i, s, f, lp, instructions) -> {
            instructions.add(pop("rax"));
            instructions.add("mov rsp, rbp", pop("rbp")).add(ret());
        });
        instructionGenerators.put("areturn", (st, i, s, f, lp, instructions) -> {
            instructions.add(pop("rax"));
            instructions.add("mov rsp, rbp", pop("rbp")).add(ret());
        });
        instructionGenerators.put("bpush", (st, i, s, f, lp, instructions) -> instructions.add("push word " + i.getData()[0]));
        instructionGenerators.put("spush", (st, i, s, f, lp, instructions) -> instructions.add("push word " + i.getData()[0]));
        instructionGenerators.put("ipush", (st, i, s, f, lp, instructions) -> instructions.add(push(((Number)i.getData()[0]).longValue())));
        instructionGenerators.put("lpush", (st, i, s, f, lp, instructions) -> {
            long v = ((Number) i.getData()[0]).longValue();
            if(v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
                instructions.add("mov rax, " + v);
                instructions.add(push("rax"));
                return;
            }
            instructions.add(push(v));
        });
        instructionGenerators.put("push_false", (st, i, s, f, lp, instructions) -> instructions.add(push(0)));
        instructionGenerators.put("push_true", (st, i, s, f, lp, instructions) -> instructions.add(push(1)));
        instructionGenerators.put("null", (st, i, s, f, lp, instructions) -> instructions.add(push(0)));
        instructionGenerators.put("bload_local", (st, i, s, f, lp, instructions) -> instructions.add("xor cx, cx", "mov cl, [rbp-" + i.getData()[0] + "]", push("cx")));
        instructionGenerators.put("sload_local", (st, i, s, f, lp, instructions) -> instructions.add("push word [rbp-" + i.getData()[0] + "]"));
        instructionGenerators.put("iload_local", (st, i, s, f, lp, instructions) -> instructions.add("xor rcx, rcx", "mov ecx, [rbp-" + i.getData()[0] + "]", push("rcx")));
        instructionGenerators.put("lload_local", (st, i, s, f, lp, instructions) -> instructions.add("push qword [rbp-" + i.getData()[0] + "]"));
        instructionGenerators.put("aload_local", (st, i, s, f, lp, instructions) -> instructions.add("push qword [rbp-" + i.getData()[0] + "]"));
        instructionGenerators.put("bstore_local", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), "mov [rbp-" + i.getData()[0] + "], cl"));
        instructionGenerators.put("sstore_local", (st, i, s, f, lp, instructions) -> instructions.add("pop word [rbp-" + i.getData()[0] + "]"));
        instructionGenerators.put("istore_local", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), "mov qword [rbp-" + i.getData()[0] + "], ecx"));
        instructionGenerators.put("lstore_local", (st, i, s, f, lp, instructions) -> instructions.add("pop qword [rbp-" + i.getData()[0] + "]"));
        instructionGenerators.put("astore_local", (st, i, s, f, lp, instructions) -> instructions.add("pop qword [rbp-" + i.getData()[0] + "]"));
        instructionGenerators.put("push_string", (st, i, s, f, lp, instructions) -> instructions.add("push _string" + lp.getOrCreateStringIndex((String) i.getData()[0])));
        instructionGenerators.put("bload_array", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), "mov cl, [rdx + rcx]", push("cx")));
        instructionGenerators.put("sload_array", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), "mov cx, [rdx + rcx * 2]", push("cx")));
        instructionGenerators.put("iload_array", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), "mov ecx, [rdx + rcx * 4]", push("rcx")));
        instructionGenerators.put("lload_array", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), "mov rcx, [rdx + rcx * 8]", push("rcx")));
        instructionGenerators.put("aload_array", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), "mov rcx, [rdx + rcx * 8]", push("rcx")));
        instructionGenerators.put("bstore_array", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), pop("ax"), "mov [rdx + rcx], al"));
        instructionGenerators.put("sstore_array", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), pop("ax"), "mov [rdx + rcx * 2], ax"));
        instructionGenerators.put("istore_array", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), pop("rax"), "mov [rdx + rcx * 4], eax"));
        instructionGenerators.put("lstore_array", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), pop("rax"), "mov [rdx + rcx * 8], rax"));
        instructionGenerators.put("astore_array", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), pop("rax"), "mov [rdx + rcx * 8], rax"));
        instructionGenerators.put("bload_field", (st, i, s, f, lp, instructions) -> instructions.add("xor cx, cx", "mov cl, [" + getMangledName((Field) i.getData()[0]) + "]", push("cx")));
        instructionGenerators.put("sload_field", (st, i, s, f, lp, instructions) -> instructions.add("mov cx, [" + getMangledName((Field) i.getData()[0]) + "]", push("cx")));
        instructionGenerators.put("iload_field", (st, i, s, f, lp, instructions) -> instructions.add("mov ecx, [" + getMangledName((Field) i.getData()[0]) + "]", push("rcx")));
        instructionGenerators.put("lload_field", (st, i, s, f, lp, instructions) -> instructions.add("mov rcx, [" + getMangledName((Field) i.getData()[0]) + "]", push("rcx")));
        instructionGenerators.put("aload_field", (st, i, s, f, lp, instructions) -> instructions.add("mov rcx, [" + getMangledName((Field) i.getData()[0]) + "]", push("rcx")));
        instructionGenerators.put("bstore_field", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), "mov [" + getMangledName((Field) i.getData()[0]) + "], cl"));
        instructionGenerators.put("sstore_field", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), "mov [" + getMangledName((Field) i.getData()[0]) + "], cx"));
        instructionGenerators.put("istore_field", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), "mov [" + getMangledName((Field) i.getData()[0]) + "], ecx"));
        instructionGenerators.put("lstore_field", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), "mov [" + getMangledName((Field) i.getData()[0]) + "], rcx"));
        instructionGenerators.put("astore_field", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), "mov [" + getMangledName((Field) i.getData()[0]) + "], rcx"));
        instructionGenerators.put("class_field_load", (st, i, s, f, lp, instructions) -> {
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

            int size;
            if(field.type() instanceof ClassType) size = 8;
            else if(field.type() instanceof BooleanType) size = 8;
            else if(field.type() instanceof ArrayType) size = 8;
            else if(field.type() instanceof PrimitiveType primitive) size = primitive.getSize();
            else throw new IllegalStateException("Cannot get size of type '" + field.type().getName() + "'");

            instructions.add(pop("rcx"));
            if(size == 1) {
                instructions.add("xor dx, dx");
                instructions.add("mov dl, [rcx+" + offset + "]");
                instructions.add(push("dx"));
            } else if(size == 2) instructions.add("push word [rcx+" + offset + "]");
            else if(size == 4) {
                instructions.add("xor edx, edx");
                instructions.add("mov edx, [rcx+" + offset + "]");
                instructions.add(push("rdx"));
            }
            else if(size == 8) instructions.add("push qword [rcx+" + offset + "]");
            else throw new IllegalStateException("Unsupported size " + size);

        });
        instructionGenerators.put("class_field_store", (st, i, s, f, lp, instructions) -> {
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

            int size;
            if(field.type() instanceof ClassType) size = 8;
            else if(field.type() instanceof BooleanType) size = 8;
            else if(field.type() instanceof ArrayType) size = 8;
            else if(field.type() instanceof PrimitiveType primitive) size = primitive.getSize();
            else throw new IllegalStateException("Cannot get size of type '" + field.type().getName() + "'");

            instructions.add(pop("rcx"));
            if(size == 1) {
                instructions.add(pop("dx"));
                instructions.add("mov [rcx+" + offset + "], dl");
            }
            else if(size == 2) instructions.add("pop word [rcx+" + offset + "]");
            else if(size == 4) {
                instructions.add(pop("rdx"));
                instructions.add("mov [rcx+" + offset + "], edx");
            }
            else if(size == 8) instructions.add("pop qword [rcx+" + offset + "]");
            else throw new IllegalStateException("Unsupported size " + size);

        });
        instructionGenerators.put("bneq", (st, i, s, f, lp, instructions) -> instructions.add(pop("dx"), pop("cx"), "xor ax, ax", "cmp cl, dl", "mov cx, 1", "cmovne ax, cx", push("ax")));
        instructionGenerators.put("sneq", (st, i, s, f, lp, instructions) -> instructions.add(pop("dx"), pop("cx"), "xor ax, ax", "cmp cx, dx", "mov cx, 1", "cmovne ax, cx", push("ax")));
        instructionGenerators.put("ineq", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmovne rax, rcx", push("rax")));
        instructionGenerators.put("lneq", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmovne rax, rcx", push("rax")));
        instructionGenerators.put("aneq", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmovne rax, rcx", push("rax")));
        instructionGenerators.put("beq", (st, i, s, f, lp, instructions) -> instructions.add(pop("dx"), pop("cx"), "xor ax, ax", "cmp cl, dl", "mov cx, 1", "cmove ax, cx", push("ax")));
        instructionGenerators.put("seq", (st, i, s, f, lp, instructions) -> instructions.add(pop("dx"), pop("cx"), "xor ax, ax", "cmp cx, dx", "mov cx, 1", "cmove ax, cx", push("ax")));
        instructionGenerators.put("ieq", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmove rax, rcx", push("rax")));
        instructionGenerators.put("leq", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmove rax, rcx", push("rax")));
        instructionGenerators.put("aeq", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmove rax, rcx", push("rax")));
        instructionGenerators.put("lltu", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmovb rax, rcx", push("rax")));
        instructionGenerators.put("lleu", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmovbe rax, rcx", push("rax")));
        instructionGenerators.put("lgtu", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmova rax, rcx", push("rax")));
        instructionGenerators.put("lgeu", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmovae rax, rcx", push("rax")));
        instructionGenerators.put("llt", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmovl rax, rcx", push("rax")));
        instructionGenerators.put("lle", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmovle rax, rcx", push("rax")));
        instructionGenerators.put("lgt", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmovg rax, rcx", push("rax")));
        instructionGenerators.put("lge", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmovge rax, rcx", push("rax")));
        instructionGenerators.put("iltu", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmovb rax, rcx", push("rax")));
        instructionGenerators.put("ileu", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmovbe rax, rcx", push("rax")));
        instructionGenerators.put("igtu", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmova rax, rcx", push("rax")));
        instructionGenerators.put("igeu", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmovae rax, rcx", push("rax")));
        instructionGenerators.put("ilt", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmovl rax, rcx", push("rax")));
        instructionGenerators.put("ile", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmovle rax, rcx", push("rax")));
        instructionGenerators.put("igt", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmovg rax, rcx", push("rax")));
        instructionGenerators.put("ige", (st, i, s, f, lp, instructions) -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmovge rax, rcx", push("rax")));
        instructionGenerators.put("neqjmp", (st, i, s, f, lp, instructions) -> instructions.add(pop("rax"), "cmp rax, 0", "je " + getMangledName(f) + "." + (s.currentIndex() - 1 + ((Number)i.getData()[0]).shortValue())));
        instructionGenerators.put("eqjmp", (st, i, s, f, lp, instructions) -> instructions.add(pop("rax"), "cmp rax, 0", "jne " + getMangledName(f) + "." + (s.currentIndex() - 1 + ((Number)i.getData()[0]).shortValue())));
        instructionGenerators.put("jmp", (st, i, s, f, lp, instructions) -> instructions.add("jmp " + getMangledName(f) + "." + (s.currentIndex() - 1 + ((Number)i.getData()[0]).shortValue())));
        instructionGenerators.put("badd", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), pop("dx"), "add cl, dl", push("cx")));
        instructionGenerators.put("sadd", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), pop("dx"), "add cx, dx", push("cx")));
        instructionGenerators.put("iadd", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), "add ecx, edx", push("rcx")));
        instructionGenerators.put("ladd", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), "add rcx, rdx", push("rcx")));
        instructionGenerators.put("bsub", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), pop("dx"), "sub dl, cl", push("dx")));
        instructionGenerators.put("ssub", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), pop("dx"), "sub dx, cx", push("dx")));
        instructionGenerators.put("isub", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), "sub edx, ecx", push("rdx")));
        instructionGenerators.put("lsub", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), "sub rdx, rcx", push("rdx")));
        instructionGenerators.put("band", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), pop("dx"), "and cl, dl", push("cx")));
        instructionGenerators.put("sand", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), pop("dx"), "and cx, dx", push("cx")));
        instructionGenerators.put("iand", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), "and ecx, edx", push("rcx")));
        instructionGenerators.put("land", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), "and rcx, rdx", push("rcx")));
        instructionGenerators.put("bshift_left", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), pop("dx"), "shl dl, cl", push("dx")));
        instructionGenerators.put("sshift_left", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), pop("dx"), "shl dx, cl", push("dx")));
        instructionGenerators.put("ishift_left", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), "shl edx, cl", push("rdx")));
        instructionGenerators.put("lshift_left", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), "shl rdx, cl", push("rdx")));
        instructionGenerators.put("bshift_right", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), pop("dx"), "shr dl, cl", push("dx")));
        instructionGenerators.put("sshift_right", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), pop("dx"), "shr dx, cl", push("dx")));
        instructionGenerators.put("ishift_right", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), "shr edx, cl", push("rdx")));
        instructionGenerators.put("lshift_right", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), "shr rdx, cl", push("rdx")));
        instructionGenerators.put("bmul", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), pop("dx"), "imul cl, dl", push("cx")));
        instructionGenerators.put("smul", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), pop("dx"), "imul cx, dx", push("cx")));
        instructionGenerators.put("imul", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), "imul ecx, edx", push("rcx")));
        instructionGenerators.put("lmul", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), "imul rcx, rdx", push("rcx")));
        instructionGenerators.put("bmulu", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), pop("ax"), "mul cl", push("ax")));
        instructionGenerators.put("smulu", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), pop("ax"), "mul cx", push("ax")));
        instructionGenerators.put("imulu", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rax"), "mul ecx", push("rax")));
        instructionGenerators.put("lmulu", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rax"), "mul rcx", push("rax")));
        instructionGenerators.put("idiv", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rax"), "xor rdx, rdx", "idiv ecx", push("rax")));
        instructionGenerators.put("ldiv", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rax"), "xor rdx, rdx", "idiv rcx", push("rax")));
        instructionGenerators.put("idivu", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rax"), "xor rdx, rdx", "div ecx", push("rax")));
        instructionGenerators.put("ldivu", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rax"), "xor rdx, rdx", "div rcx", push("rax")));
        instructionGenerators.put("imod", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rax"), "xor rdx, rdx", "idiv ecx", push("rdx")));
        instructionGenerators.put("lmod", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rax"), "xor rdx, rdx", "idiv rcx", push("rdx")));
        instructionGenerators.put("imodu", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rax"), "xor rdx, rdx", "div ecx", push("rdx")));
        instructionGenerators.put("lmodu", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rax"), "xor rdx, rdx", "div rcx", push("rdx")));
        instructionGenerators.put("bor", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), pop("dx"), "or cl, dl", push("cx")));
        instructionGenerators.put("sor", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), pop("dx"), "or cx, dx", push("cx")));
        instructionGenerators.put("ior", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), "or ecx, edx", push("rcx")));
        instructionGenerators.put("lor", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), pop("rdx"), "or rcx, rdx", push("rcx")));
        instructionGenerators.put("bpop", (st, i, s, f, lp, instructions) -> instructions.add("add rsp, 2"));
        instructionGenerators.put("spop", (st, i, s, f, lp, instructions) -> instructions.add("add rsp, 2"));
        instructionGenerators.put("ipop", (st, i, s, f, lp, instructions) -> instructions.add("add rsp, 8"));
        instructionGenerators.put("lpop", (st, i, s, f, lp, instructions) -> instructions.add("add rsp, 8"));
        instructionGenerators.put("apop", (st, i, s, f, lp, instructions) -> instructions.add("add rsp, 8"));
        instructionGenerators.put("bdup", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), push("cx"), push("cx")));
        instructionGenerators.put("sdup", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), push("cx"), push("cx")));
        instructionGenerators.put("idup", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), push("rcx"), push("rcx")));
        instructionGenerators.put("ldup", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), push("rcx"), push("rcx")));
        instructionGenerators.put("adup", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), push("rcx"), push("rcx")));
        instructionGenerators.put("bcasti", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), "movsx ecx, cx", push("rcx")));
        instructionGenerators.put("bcastl", (st, i, s, f, lp, instructions) -> instructions.add(pop("cx"), "movsx rcx, cx", push("rcx")));
        instructionGenerators.put("icastl", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), "movsxd rcx, ecx", push("rcx")));
        instructionGenerators.put("lcasts", (st, i, s, f, lp, instructions) -> instructions.add(pop("rcx"), push("cx")));
        instructionGenerators.put("invoke", (st, i, s, func, lp, instructions) -> invokeFunction(st, (Function) i.getData()[0], false, instructions));
        instructionGenerators.put("invoke_class", (st, i, s, func, lp, instructions) -> {
            var method = (Method) i.getData()[0];
            invokeFunction(st, new Function(method.namespace(), method.modifiers(), method.className() + "." + method.name(), method.signature(), method.code()), true, instructions);
        });
    }

    private static void invokeFunction(State state, Function f, boolean methodLike, InstructionList instructions) {
        boolean isSyscall = false;
        if(FunctionModifiers.isEmptyCode(f.modifiers())) {
            boolean isFound = false;
            if(Objects.equals(f.namespace(), "zprol.lang.io.direct")) {
                if(f.name().equals("inb") && f.signature().toString().equals("uB(uS)")) instructions.add("pop dx").add("in al, dx").add("push ax");
                else if(f.name().equals("ins") && f.signature().toString().equals("uS(uS)")) instructions.add("pop dx").add("in ax, dx").add("push ax");
                else if(f.name().equals("inw") && f.signature().toString().equals("uI(uS)")) instructions.add("pop dx").add("in eax, dx").add("push rax");
                else if(f.name().equals("outb") && f.signature().toString().equals("V(uSuB)")) instructions.add("pop ax").add("pop dx").add("out dx, al");
                else if(f.name().equals("outs") && f.signature().toString().equals("V(uSuS)")) instructions.add("pop ax").add("pop dx").add("out dx, ax");
                else if(f.name().equals("outw") && f.signature().toString().equals("V(uSuI)")) instructions.add("pop rax").add("pop dx").add("out dx, eax");
                else isFound = true;
            }
            if(Objects.equals(f.namespace(), "zprol.lang.linux.amd64")) {
                if(f.name().equals("syscall")) {
                    isSyscall = true;
                    isFound = true;
                }
            }
            if(!isFound) return;
        }

        var params = f.signature().parameters();
        int off = methodLike ? 1 : 0;
        for(int x = params.length - 1 + off; x>=0; x--) {
            if(x == params.length && methodLike) {
                instructions.add(pop(isSyscall ? SYSCALL_REGISTERS_64[x] : CALL_REGISTERS_64[x]));
            }else {
                var param = params[x];
                if (param instanceof PrimitiveType primitive) {
                    if (primitive.getSize() == 1 || primitive.getSize() == 2) {
                        instructions.add(pop(isSyscall ? SYSCALL_REGISTERS_16[x] : CALL_REGISTERS_16[x]));
                    } else if (primitive.getSize() == 4 || primitive.getSize() == 8) {
                        instructions.add(pop(isSyscall ? SYSCALL_REGISTERS_64[x] : CALL_REGISTERS_64[x]));
                    } else {
                        throw new NotImplementedException("Not implemented size " + primitive.getSize());
                    }
                } else {
                    instructions.add(pop(isSyscall ? SYSCALL_REGISTERS_64[x] : CALL_REGISTERS_64[x]));
                }
            }
        }

        if (FunctionModifiers.isEmptyCode(f.modifiers())) {
            if (isSyscall) {
                instructions.add("syscall");
            } else {
                throw new FunctionNotDefinedException("Unknown native function " + f.name());
            }
        } else {
            var mangledName = getMangledName(f);
            if(!ALL_FUNCTIONS) {
                if(!state.definedFunctions.contains(mangledName)) {
                    state.nextFunctions.add(f);
                }
            }
            instructions.add(call(getMangledName(f), f));
        }

        var ret = f.signature().returnType();
        if(ret instanceof PrimitiveType primitive) {
            if (primitive.getSize() == 0) {
            } else if (primitive.getSize() == 1 || primitive.getSize() == 2) {
                instructions.add(push("ax"));
            } else if (primitive.getSize() == 4 || primitive.getSize() == 8) {
                instructions.add(push("rax"));
            } else {
                throw new NotImplementedException("Not implemented size " + primitive.getSize());
            }
        }else if(ret instanceof ClassType) {
            instructions.add(push("rax"));
        }else if(ret instanceof BooleanType) {
            instructions.add(push("rax"));
        }else if(ret instanceof ArrayType) {
            instructions.add(push("rax"));
        }else if(!(ret instanceof VoidType)) {
            throw new IllegalStateException("Cannot finish generating call instruction because of an unknown type '" + ret.getName() + "'");
        }
    }

    private static void writeFunction(State state, Function function, boolean methodLike, ConstantPool localConstantPool, InstructionList assembly) {
        assembly.add(push("rbp"));
        assembly.add("mov rbp, rsp");
        assembly.add("sub rsp, " + function.code().getLocalsSize());
        int localsIndex = 0;
        var parameters = function.signature().parameters();
        int off = methodLike ? 1 : 0;
        for (int i = 0; i < parameters.length + off; i++) {
            if(methodLike && i == 0) {
                localsIndex += 8;
                assembly.add("mov [rbp-" + localsIndex + "], " + CALL_REGISTERS_64[i]);
            }else {
                var param = parameters[i - off];
                if (param instanceof PrimitiveType primitive) {
                    if (primitive.getSize() == 1 || primitive.getSize() == 2) {
                        localsIndex += 2;
                        assembly.add("mov [rbp-" + localsIndex + "], " + CALL_REGISTERS_16[i]);
                    } else if (primitive.getSize() == 4 || primitive.getSize() == 8) {
                        localsIndex += 8;
                        assembly.add("mov [rbp-" + localsIndex + "], " + CALL_REGISTERS_64[i]);
                    } else {
                        localsIndex += primitive.getSize();
                    }
                } else {
                    localsIndex += 8;
                    assembly.add("mov [rbp-" + localsIndex + "], " + CALL_REGISTERS_64[i]);
                }
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
//            assembly.add(new Instruction("; " + instruction.getName()));
            var generator = instructionGenerators.get(instruction.getName());
            if(generator != null) {
                generator.generateInstruction(state, instruction, instructions, function, localConstantPool, assembly);
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

        public InstructionList add(Object... instr) {
            for(Object inst : instr) {
                if(inst instanceof Instruction i) {
                    add(i);
                }else {
                    add(new Instruction(inst.toString()));
                }
            }
            return this;
        }

    }

    private interface InstructionGenerator {
        public void generateInstruction(State st, IBytecodeInstruction i, SeekIterator<IBytecodeInstruction> s, Function function, ConstantPool lp, InstructionList instructions);
    }

    private static class State {
        private final ArrayList<String> definedFunctions = new ArrayList<>();
        private final ArrayDeque<Object> nextFunctions = new ArrayDeque<>();
    }

    public static final boolean ALL_FUNCTIONS = Objects.equals(System.getenv("ALL_FUNCTIONS"), "true");

    public void generate(DataOutputStream outStream, GeneratedData generated) throws IOException {
        InstructionList assembly = new InstructionList();

        assembly.add("global _start");
        assembly.add("_start:");
        for(var function : generated.functions) {
            if(FunctionModifiers.isEmptyCode(function.modifiers())) continue;
            if(!function.name().equals(".init")) continue;
            assembly.add("call " + getMangledName(function));
        }
        assembly.add("call " + getMangledName(null, "main", new FunctionSignature(Types.getTypeFromDescriptor("V"))));
        assembly.add("mov rax, 60");
        assembly.add("mov rdi, 0");
        assembly.add("syscall");
        ConstantPool localConstantPool = new ConstantPool();
        State state = new State();
        if(ALL_FUNCTIONS) {
            for(var function : generated.functions) {
                if(FunctionModifiers.isEmptyCode(function.modifiers())) continue;

                assembly.add(new FunctionStartInstruction(function));
                writeFunction(state, function, false, localConstantPool, assembly);
            }
            for(var clazz : generated.classes) {
                for (var method : clazz.methods()) {
                    if (FunctionModifiers.isEmptyCode(method.modifiers())) continue;

                    assembly.add(getMangledName(method) + ":");
                    writeFunction(state, new Function(method.namespace(), method.modifiers(), method.className() + "." + method.name(), method.signature(), method.code()), true, localConstantPool, assembly);
                }
            }
        }else {
            Function mainFunction = generated.getFunction(null, "main", new FunctionSignature(Types.getTypeFromDescriptor("V")).toString());

            assembly.add(new FunctionStartInstruction(mainFunction));
            writeFunction(state, mainFunction, false, localConstantPool, assembly);
            while(!state.nextFunctions.isEmpty()) {
                var current = state.nextFunctions.pop();
                if(current instanceof Function f) {
                    if(FunctionModifiers.isEmptyCode(f.modifiers())) continue;
                    state.definedFunctions.add(getMangledName(f));

                    assembly.add(new FunctionStartInstruction(f));
                    writeFunction(state, f, false, localConstantPool, assembly);
                }else if(current instanceof Method m) {
                    if (FunctionModifiers.isEmptyCode(m.modifiers())) continue;
                    state.definedFunctions.add(getMangledName(m));

                    assembly.add(getMangledName(m) + ":");
                    writeFunction(state, new Function(m.namespace(), m.modifiers(), m.className() + "." + m.name(), m.signature(), m.code()), true, localConstantPool, assembly);
                }
            }
        }

        for (var field : generated.fields) {
            if(field.defaultValue() == null) continue;
            if(field.type() instanceof ClassType clz && clz.normalName().equals("zprol.lang.String")) {
                localConstantPool.getOrCreateStringIndex((String) field.defaultValue().value());
            }
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

        if(generated.fields.size() != 0) {
            boolean shownBSS = false;
            for (var field : generated.fields) {
                if(field.defaultValue() != null) continue;
                int size = 8;
                if (field.type() instanceof PrimitiveType prim) {
                    size = prim.size;
                }
                if(!shownBSS) {
                    assembly.add("section .bss");
                    shownBSS = true;
                }
                assembly.add(getMangledName(field) + ": resb " + size);
            }

            boolean shownROData = false;
            for (var field : generated.fields) {
                if(field.defaultValue() == null) continue;
                if(!shownROData) {
                    assembly.add("section .rodata");
                    shownROData = true;
                }
                if (field.type() instanceof PrimitiveType prim) {
                    int size = prim.size;
                    switch(size) {
                        case 1 -> assembly.add(getMangledName(field) + ": db " + field.defaultValue().value());
                        case 2 -> assembly.add(getMangledName(field) + ": dw " + field.defaultValue().value());
                        case 4 -> assembly.add(getMangledName(field) + ": dd " + field.defaultValue().value());
                        case 8 -> assembly.add(getMangledName(field) + ": dq " + field.defaultValue().value());
                    }
                }else if(field.type() instanceof NullType) {
                    assembly.add(getMangledName(field) + ": dq 0");
                }else if(field.type() instanceof ClassType clz && clz.normalName().equals("zprol.lang.String")) {
                    var s = (String) field.defaultValue().value();
                    index = 1;
                    for(var constantPoolEntry : localConstantPool.entries) {
                        if(constantPoolEntry instanceof ConstantPoolEntry.StringEntry str) {
                            if(str.getString().equals(s)) {
                                assembly.add(getMangledName(field) + ": dq _string" + index);
                                break;
                            }
                        }
                        index++;
                    }
                }else {
                    throw new IllegalStateException("Unexpected type: " + field.type());
                }
            }
        }

        // -O embed value return functions
        HashMap<Function, ValueReturnInstruction> valueReturnFunctions = new HashMap<>();
        for(int i = 0; i<assembly.instructions.size(); i++) {
            if(assembly.instructions.get(i) instanceof FunctionStartInstruction func) {
                if(func.function.signature().parameters().length == 0 && func.function.signature().returnType() instanceof PrimitiveType prim && prim.getSize() == 8) {
                    if (assembly.instructions.get(i + 1) instanceof PushNumberInstruction push) {
                        if (assembly.instructions.get(i + 2) instanceof PopInstruction pop) {
                            if (pop.register.equals("rax")) {
                                if (assembly.instructions.get(i + 3) instanceof ReturnInstruction) {
                                    assembly.instructions.remove(i);
                                    assembly.instructions.remove(i);
                                    assembly.instructions.remove(i);
                                    assembly.instructions.remove(i);
                                    i -= 4;
                                    valueReturnFunctions.put(func.function, new ValueReturnInstruction(push.number));
                                }
                            }
                        }
                    }
                }
            }
        }
        for(int i = 0; i<assembly.instructions.size(); i++) {
            if(assembly.instructions.get(i) instanceof CallInstruction call) {
                var v = valueReturnFunctions.get(call.function);
                if(v != null) {
                    if(assembly.instructions.get(i + 1) instanceof PushInstruction push && push.register.equals("rax")) {
                        assembly.instructions.remove(i + 1);
                        assembly.instructions.remove(i);
                        assembly.instructions.add(i, new Instruction("push " + v.value));
                    }else {
                        assembly.instructions.remove(i);
                        assembly.instructions.add(i, new Instruction("mov rax, " + v.value));
                    }
                }
            }
        }

        // -O merge pop,push to mov
        for(int i = 0; i<assembly.instructions.size(); i++) {
            if(assembly.instructions.get(i) instanceof PushInstruction push) {
                if(push.register.startsWith("r")) {
                    if(assembly.instructions.get(i + 1) instanceof PopInstruction pop) {
                        if(pop.register.startsWith("r")) {
                            assembly.instructions.remove(i);
                            assembly.instructions.remove(i);
                            if(!push.register.equals(pop.register)) {
                                assembly.instructions.add(i, new Instruction("mov " + pop.register + ", " + push.register));
                            }
                        }
                    }
                }
            }else if(assembly.instructions.get(i) instanceof PushNumberInstruction push) {
                if(assembly.instructions.get(i + 1) instanceof PopInstruction pop) {
                    if(pop.register.startsWith("r")) {
                        assembly.instructions.remove(i);
                        assembly.instructions.remove(i);
                        assembly.instructions.add(i, new Instruction("mov " + pop.register + ", " + push.number));
                    }
                }
            }
        }


        outStream.writeBytes(assembly.instructions.stream().map(Instruction::data).collect(Collectors.joining("\n")));
    }

    public static String getMangledName(Function func) {
        return getMangledName(func.namespace(), func.name(), func.signature());
    }

    public static String getMangledName(Method func) {
        return getMangledName(func.namespace(), func.className(), func.name(), func.signature());
    }

    public static String getMangledName(Field field) {
        return getMangledName(field.namespace(), field.name(), field.type());
    }

    public static String getMangledName(String namespace, String name, FunctionSignature signature) {
        String r = (namespace != null ? namespace.replace('.', '$') + "$" : "") + name + "$" + signature.toString().replace("(", "").replace(")", "").replace(";", "").replace("[", "r");
        if(r.startsWith(".")) {
            return "@" + r;
        }
        return r;
    }

    public static String getMangledName(String namespace, String className, String name, FunctionSignature signature) {
        String r = (namespace != null ? namespace.replace('.', '$') + "$" : "") + className + "." + name + "$" + signature.toString().replace("(", "").replace(")", "").replace(";", "").replace("[", "r");
        if(r.startsWith(".")) {
            return "@" + r;
        }
        return r;
    }

    public static String getMangledName(String namespace, String name, Type type) {
        String r = (namespace != null ? namespace.replace('.', '$') + "$" : "") + name + "$" + type.getDescriptor().replace(";", "").replace("[", "r");
        if(r.startsWith(".")) {
            return "@" + r;
        }
        return r;
    }

}
