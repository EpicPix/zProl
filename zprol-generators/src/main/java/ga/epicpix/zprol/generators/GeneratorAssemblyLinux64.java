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

    public static void markField(State state, Field field) {
        if(!ALL_FUNCTIONS) {
            String n = getMangledName(field);
            if(!state.definedFields.contains(n)) {
                state.nextFields.add(field);
                state.definedFields.add(n);
            }
        }
    }

    private static final String[] CALL_REGISTERS_64 = new String[] {"rdi", "rsi", "rdx", "rcx", "r8", "r9"};
    private static final String[] CALL_REGISTERS_16 = new String[] {"di", "sx", "dx", "cx", "r8w", "r9w"};
    private static final String[] SYSCALL_REGISTERS_64 = new String[] {"rax", "rdi", "rsi", "rdx", "r10", "r8", "r9"};
    private static final String[] SYSCALL_REGISTERS_16 = new String[] {"ax", "di", "sx", "dx", "r10w", "r8w", "r9w"};

    public static void generateInstruction(State st, IBytecodeInstruction i, SeekIterator<IBytecodeInstruction> s, Function f, ConstantPool lp, InstructionList instructions) {
        String name = i.getName();
        switch(name) {
            case "vreturn" -> instructions.add("mov rsp, rbp", pop("rbp")).add(ret());
            case "breturn", "sreturn" -> instructions.add(pop("ax"), "mov rsp, rbp", pop("rbp"), ret());
            case "ireturn", "lreturn", "areturn" -> instructions.add(pop("rax"), "mov rsp, rbp", pop("rbp"), ret());
            case "bpush", "spush" -> instructions.add("push word " + i.getData()[0]);
            case "ipush" -> instructions.add(push(((Number)i.getData()[0]).longValue()));
            case "lpush" -> {
                long v = ((Number) i.getData()[0]).longValue();
                if(v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
                    instructions.add("mov rax, " + v);
                    instructions.add(push("rax"));
                    return;
                }
                instructions.add(push(v));
            }
            case "push_false", "null" -> instructions.add(push(0));
            case "push_true" -> instructions.add(push(1));
            case "bload_local" -> instructions.add("xor cx, cx", "mov cl, [rbp-" + i.getData()[0] + "]", push("cx"));
            case "sload_local" -> instructions.add("push word [rbp-" + i.getData()[0] + "]");
            case "iload_local" -> instructions.add("xor rcx, rcx", "mov ecx, [rbp-" + i.getData()[0] + "]", push("rcx"));
            case "lload_local" -> instructions.add("push qword [rbp-" + i.getData()[0] + "]");
            case "aload_local" -> instructions.add("push qword [rbp-" + i.getData()[0] + "]");
            case "bstore_local" -> instructions.add(pop("cx"), "mov [rbp-" + i.getData()[0] + "], cl");
            case "sstore_local" -> instructions.add("pop word [rbp-" + i.getData()[0] + "]");
            case "istore_local" -> instructions.add(pop("rcx"), "mov qword [rbp-" + i.getData()[0] + "], ecx");
            case "lstore_local" -> instructions.add("pop qword [rbp-" + i.getData()[0] + "]");
            case "astore_local" -> instructions.add("pop qword [rbp-" + i.getData()[0] + "]");
            case "push_string" -> instructions.add("push _string" + lp.getOrCreateStringIndex((String) i.getData()[0]));
            case "bload_array" -> instructions.add(pop("rcx"), pop("rdx"), "mov cl, [rdx + rcx]", push("cx"));
            case "sload_array" -> instructions.add(pop("rcx"), pop("rdx"), "mov cx, [rdx + rcx * 2]", push("cx"));
            case "iload_array" -> instructions.add(pop("rcx"), pop("rdx"), "mov ecx, [rdx + rcx * 4]", push("rcx"));
            case "lload_array" -> instructions.add(pop("rcx"), pop("rdx"), "mov rcx, [rdx + rcx * 8]", push("rcx"));
            case "aload_array" -> instructions.add(pop("rcx"), pop("rdx"), "mov rcx, [rdx + rcx * 8]", push("rcx"));
            case "bstore_array" -> instructions.add(pop("rcx"), pop("rdx"), pop("ax"), "mov [rdx + rcx], al");
            case "sstore_array" -> instructions.add(pop("rcx"), pop("rdx"), pop("ax"), "mov [rdx + rcx * 2], ax");
            case "istore_array" -> instructions.add(pop("rcx"), pop("rdx"), pop("rax"), "mov [rdx + rcx * 4], eax");
            case "lstore_array" -> instructions.add(pop("rcx"), pop("rdx"), pop("rax"), "mov [rdx + rcx * 8], rax");
            case "astore_array" -> instructions.add(pop("rcx"), pop("rdx"), pop("rax"), "mov [rdx + rcx * 8], rax");
            case "bload_field" -> {
                markField(st, (Field) i.getData()[0]);
                instructions.add("xor cx, cx", "mov cl, [" + getMangledName((Field) i.getData()[0]) + "]", push("cx"));
            }
            case "sload_field" -> {
                markField(st, (Field) i.getData()[0]);
                instructions.add("mov cx, [" + getMangledName((Field) i.getData()[0]) + "]", push("cx"));
            }
            case "iload_field" -> {
                markField(st, (Field) i.getData()[0]);
                instructions.add("mov ecx, [" + getMangledName((Field) i.getData()[0]) + "]", push("rcx"));
            }
            case "lload_field", "aload_field" -> {
                markField(st, (Field) i.getData()[0]);
                instructions.add("mov rcx, [" + getMangledName((Field) i.getData()[0]) + "]", push("rcx"));
            }
            case "bstore_field" -> {
                markField(st, (Field) i.getData()[0]);
                instructions.add(pop("cx"), "mov [" + getMangledName((Field) i.getData()[0]) + "], cl");
            }
            case "sstore_field" -> {
                markField(st, (Field) i.getData()[0]);
                instructions.add(pop("cx"), "mov [" + getMangledName((Field) i.getData()[0]) + "], cx");
            }
            case "istore_field" -> {
                markField(st, (Field) i.getData()[0]);
                instructions.add(pop("rcx"), "mov [" + getMangledName((Field) i.getData()[0]) + "], ecx");
            }
            case "lstore_field", "astore_field" -> {
                markField(st, (Field) i.getData()[0]);
                instructions.add(pop("rcx"), "mov [" + getMangledName((Field) i.getData()[0]) + "], rcx");
            }
            case "class_field_load"  -> {
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
            }
            case "class_field_store" -> {
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
            }
            case "bneq" -> instructions.add(pop("dx"), pop("cx"), "xor ax, ax", "cmp cl, dl", "mov cx, 1", "cmovne ax, cx", push("ax"));
            case "sneq" -> instructions.add(pop("dx"), pop("cx"), "xor ax, ax", "cmp cx, dx", "mov cx, 1", "cmovne ax, cx", push("ax"));
            case "ineq" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmovne rax, rcx", push("rax"));
            case "lneq" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmovne rax, rcx", push("rax"));
            case "aneq" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmovne rax, rcx", push("rax"));
            case "beq" -> instructions.add(pop("dx"), pop("cx"), "xor ax, ax", "cmp cl, dl", "mov cx, 1", "cmove ax, cx", push("ax"));
            case "seq" -> instructions.add(pop("dx"), pop("cx"), "xor ax, ax", "cmp cx, dx", "mov cx, 1", "cmove ax, cx", push("ax"));
            case "ieq" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmove rax, rcx", push("rax"));
            case "leq" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmove rax, rcx", push("rax"));
            case "aeq" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmove rax, rcx", push("rax"));
            case "lltu" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmovb rax, rcx", push("rax"));
            case "lleu" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmovbe rax, rcx", push("rax"));
            case "lgtu" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmova rax, rcx", push("rax"));
            case "lgeu" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmovae rax, rcx", push("rax"));
            case "llt" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmovl rax, rcx", push("rax"));
            case "lle" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmovle rax, rcx", push("rax"));
            case "lgt" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmovg rax, rcx", push("rax"));
            case "lge" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp rcx, rdx", "mov rcx, 1", "cmovge rax, rcx", push("rax"));
            case "iltu" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmovb rax, rcx", push("rax"));
            case "ileu" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmovbe rax, rcx", push("rax"));
            case "igtu" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmova rax, rcx", push("rax"));
            case "igeu" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmovae rax, rcx", push("rax"));
            case "ilt" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmovl rax, rcx", push("rax"));
            case "ile" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmovle rax, rcx", push("rax"));
            case "igt" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmovg rax, rcx", push("rax"));
            case "ige" -> instructions.add(pop("rdx"), pop("rcx"), "xor rax, rax", "cmp ecx, edx", "mov rcx, 1", "cmovge rax, rcx", push("rax"));
            case "neqjmp" -> instructions.add(pop("rax"), "cmp rax, 0", "je " + getMangledName(f) + "." + (s.currentIndex() - 1 + ((Number)i.getData()[0]).shortValue()));
            case "eqjmp" -> instructions.add(pop("rax"), "cmp rax, 0", "jne " + getMangledName(f) + "." + (s.currentIndex() - 1 + ((Number)i.getData()[0]).shortValue()));
            case "jmp" -> instructions.add("jmp " + getMangledName(f) + "." + (s.currentIndex() - 1 + ((Number)i.getData()[0]).shortValue()));
            case "badd" -> instructions.add(pop("cx"), pop("dx"), "add cl, dl", push("cx"));
            case "sadd" -> instructions.add(pop("cx"), pop("dx"), "add cx, dx", push("cx"));
            case "iadd" -> instructions.add(pop("rcx"), pop("rdx"), "add ecx, edx", push("rcx"));
            case "ladd" -> instructions.add(pop("rcx"), pop("rdx"), "add rcx, rdx", push("rcx"));
            case "bsub" -> instructions.add(pop("cx"), pop("dx"), "sub dl, cl", push("dx"));
            case "ssub" -> instructions.add(pop("cx"), pop("dx"), "sub dx, cx", push("dx"));
            case "isub" -> instructions.add(pop("rcx"), pop("rdx"), "sub edx, ecx", push("rdx"));
            case "lsub" -> instructions.add(pop("rcx"), pop("rdx"), "sub rdx, rcx", push("rdx"));
            case "band" -> instructions.add(pop("cx"), pop("dx"), "and cl, dl", push("cx"));
            case "sand" -> instructions.add(pop("cx"), pop("dx"), "and cx, dx", push("cx"));
            case "iand" -> instructions.add(pop("rcx"), pop("rdx"), "and ecx, edx", push("rcx"));
            case "land" -> instructions.add(pop("rcx"), pop("rdx"), "and rcx, rdx", push("rcx"));
            case "bshift_left" -> instructions.add(pop("cx"), pop("dx"), "shl dl, cl", push("dx"));
            case "sshift_left" -> instructions.add(pop("cx"), pop("dx"), "shl dx, cl", push("dx"));
            case "ishift_left" -> instructions.add(pop("rcx"), pop("rdx"), "shl edx, cl", push("rdx"));
            case "lshift_left" -> instructions.add(pop("rcx"), pop("rdx"), "shl rdx, cl", push("rdx"));
            case "bshift_right" -> instructions.add(pop("cx"), pop("dx"), "shr dl, cl", push("dx"));
            case "sshift_right" -> instructions.add(pop("cx"), pop("dx"), "shr dx, cl", push("dx"));
            case "ishift_right" -> instructions.add(pop("rcx"), pop("rdx"), "shr edx, cl", push("rdx"));
            case "lshift_right" -> instructions.add(pop("rcx"), pop("rdx"), "shr rdx, cl", push("rdx"));
            case "bmul" -> instructions.add(pop("cx"), pop("dx"), "imul cl, dl", push("cx"));
            case "smul" -> instructions.add(pop("cx"), pop("dx"), "imul cx, dx", push("cx"));
            case "imul" -> instructions.add(pop("rcx"), pop("rdx"), "imul ecx, edx", push("rcx"));
            case "lmul" -> instructions.add(pop("rcx"), pop("rdx"), "imul rcx, rdx", push("rcx"));
            case "bmulu" -> instructions.add(pop("cx"), pop("ax"), "mul cl", push("ax"));
            case "smulu" -> instructions.add(pop("cx"), pop("ax"), "mul cx", push("ax"));
            case "imulu" -> instructions.add(pop("rcx"), pop("rax"), "mul ecx", push("rax"));
            case "lmulu" -> instructions.add(pop("rcx"), pop("rax"), "mul rcx", push("rax"));
            case "idiv" -> instructions.add(pop("rcx"), pop("rax"), "xor rdx, rdx", "idiv ecx", push("rax"));
            case "ldiv" -> instructions.add(pop("rcx"), pop("rax"), "xor rdx, rdx", "idiv rcx", push("rax"));
            case "idivu" -> instructions.add(pop("rcx"), pop("rax"), "xor rdx, rdx", "div ecx", push("rax"));
            case "ldivu" -> instructions.add(pop("rcx"), pop("rax"), "xor rdx, rdx", "div rcx", push("rax"));
            case "imod" -> instructions.add(pop("rcx"), pop("rax"), "xor rdx, rdx", "idiv ecx", push("rdx"));
            case "lmod" -> instructions.add(pop("rcx"), pop("rax"), "xor rdx, rdx", "idiv rcx", push("rdx"));
            case "imodu" -> instructions.add(pop("rcx"), pop("rax"), "xor rdx, rdx", "div ecx", push("rdx"));
            case "lmodu" -> instructions.add(pop("rcx"), pop("rax"), "xor rdx, rdx", "div rcx", push("rdx"));
            case "bor" -> instructions.add(pop("cx"), pop("dx"), "or cl, dl", push("cx"));
            case "sor" -> instructions.add(pop("cx"), pop("dx"), "or cx, dx", push("cx"));
            case "ior" -> instructions.add(pop("rcx"), pop("rdx"), "or ecx, edx", push("rcx"));
            case "lor" -> instructions.add(pop("rcx"), pop("rdx"), "or rcx, rdx", push("rcx"));
            case "bpop" -> instructions.add("add rsp, 2");
            case "spop" -> instructions.add("add rsp, 2");
            case "ipop" -> instructions.add("add rsp, 8");
            case "lpop" -> instructions.add("add rsp, 8");
            case "apop" -> instructions.add("add rsp, 8");
            case "bdup" -> instructions.add(pop("cx"), push("cx"), push("cx"));
            case "sdup" -> instructions.add(pop("cx"), push("cx"), push("cx"));
            case "idup" -> instructions.add(pop("rcx"), push("rcx"), push("rcx"));
            case "ldup" -> instructions.add(pop("rcx"), push("rcx"), push("rcx"));
            case "adup" -> instructions.add(pop("rcx"), push("rcx"), push("rcx"));
            case "bcasti" -> instructions.add(pop("cx"), "movsx ecx, cx", push("rcx"));
            case "bcastl" -> instructions.add(pop("cx"), "movsx rcx, cx", push("rcx"));
            case "icastl" -> instructions.add(pop("rcx"), "movsxd rcx, ecx", push("rcx"));
            case "lcasts" -> instructions.add(pop("rcx"), push("cx"));
            case "invoke" -> invokeFunction(st, (Function) i.getData()[0], false, instructions);
            case "invoke_class" -> {
                var method = (Method) i.getData()[0];
                invokeFunction(st, new Function(method.namespace(), method.modifiers(), method.className() + "." + method.name(), method.signature(), method.code()), true, instructions);
            }
            default -> throw new UnknownInstructionException("Unable to generate instructions for the " + i.getName() + " instruction");
        };
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
                    state.definedFunctions.add(mangledName);
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
            if(NO_OPT) {
                assembly.add(new Instruction("; " + instruction.getName()));
            }
            generateInstruction(state, instruction, instructions, function, localConstantPool, assembly);
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

    private static class State {
        private final ArrayList<String> definedFunctions = new ArrayList<>();
        private final ArrayList<String> definedFields = new ArrayList<>();
        private final ArrayDeque<Object> nextFunctions = new ArrayDeque<>();
        private final ArrayDeque<Field> nextFields = new ArrayDeque<>();
    }

    public static final boolean ALL_FUNCTIONS = Objects.equals(System.getenv("ALL_FUNCTIONS"), "true");
    public static final boolean NO_OPT = Objects.equals(System.getenv("NO_OPT"), "true");

    public void generate(DataOutputStream outStream, GeneratedData generated) throws IOException {
        InstructionList assembly = new InstructionList();

        assembly.add("global _start");
        assembly.add("section .text");
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
        List<Field> fields;
        if(ALL_FUNCTIONS) {
            fields = generated.fields;
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

                    assembly.add(new FunctionStartInstruction(f));
                    writeFunction(state, f, false, localConstantPool, assembly);
                }else if(current instanceof Method m) {
                    if (FunctionModifiers.isEmptyCode(m.modifiers())) continue;

                    assembly.add(getMangledName(m) + ":");
                    writeFunction(state, new Function(m.namespace(), m.modifiers(), m.className() + "." + m.name(), m.signature(), m.code()), true, localConstantPool, assembly);
                }
            }

            fields = new ArrayList<>();
            while(!state.nextFields.isEmpty()) {
                var field = state.nextFields.pop();
                fields.add(field);
                if(field.defaultValue() == null) continue;
                if(field.type() instanceof ClassType clz && clz.normalName().equals("zprol.lang.String")) {
                    localConstantPool.getOrCreateStringIndex((String) field.defaultValue().value());
                }
            }


        }

        boolean shownReadonlyData = false;

        if(NO_OPT) {
            assembly.add("; Non-Constant Fields");
        }

        if(fields.size() != 0) {
            boolean shownBSS = false;
            for (var field : fields) {
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
        }

        if(NO_OPT) {
            assembly.add("; Strings");
        }

        int index = 1;
        for(var constantPoolEntry : localConstantPool.entries) {
            if(constantPoolEntry instanceof ConstantPoolEntry.StringEntry str) {
                if(!shownReadonlyData) {
                    assembly.add("section .rodata");
                    shownReadonlyData = true;
                }
                assembly.add("_string" + index + ".chars" + ": db " + Arrays.stream(str.getString().chars().toArray()).mapToObj(x -> "0x" + Integer.toHexString(x)).collect(Collectors.joining(", ")));
                assembly.add("_string" + index + ": dq " + str.getString().length() + " string" + index + ".chars");
            }
            index++;
        }

        if(NO_OPT) {
            assembly.add("; Constant Fields");
        }

        if(fields.size() != 0) {
            for (var field : fields) {
                if(field.defaultValue() == null) continue;
                if(!shownReadonlyData) {
                    assembly.add("section .rodata");
                    shownReadonlyData = true;
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

        if(NO_OPT) {
            // -O embed value return functions
            HashMap<Function, ValueReturnInstruction> valueReturnFunctions = new HashMap<>();
            for(int i = 0; i < assembly.instructions.size(); i++) {
                if(assembly.instructions.get(i) instanceof FunctionStartInstruction func) {
                    if(func.function.signature().parameters().length == 0 && func.function.signature().returnType() instanceof PrimitiveType prim && prim.getSize() == 8) {
                        if(assembly.instructions.get(i + 1) instanceof PushNumberInstruction push) {
                            if(assembly.instructions.get(i + 2) instanceof PopInstruction pop) {
                                if(pop.register.equals("rax")) {
                                    if(assembly.instructions.get(i + 3) instanceof ReturnInstruction) {
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
            for(int i = 0; i < assembly.instructions.size(); i++) {
                if(assembly.instructions.get(i) instanceof CallInstruction call) {
                    var v = valueReturnFunctions.get(call.function);
                    if(v != null) {
                        if(assembly.instructions.get(i + 1) instanceof PushInstruction push && push.register.equals("rax")) {
                            assembly.instructions.remove(i + 1);
                            assembly.instructions.remove(i);
                            assembly.instructions.add(i, new Instruction("push " + v.value));
                        } else {
                            assembly.instructions.remove(i);
                            assembly.instructions.add(i, new Instruction("mov rax, " + v.value));
                        }
                    }
                }
            }

            // -O merge pop,push to mov
            for(int i = 0; i < assembly.instructions.size(); i++) {
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
                } else if(assembly.instructions.get(i) instanceof PushNumberInstruction push) {
                    if(assembly.instructions.get(i + 1) instanceof PopInstruction pop) {
                        if(pop.register.startsWith("r")) {
                            assembly.instructions.remove(i);
                            assembly.instructions.remove(i);
                            assembly.instructions.add(i, new Instruction("mov " + pop.register + ", " + push.number));
                        }
                    }
                }
            }
        }


        StringJoiner joiner = new StringJoiner("\n");
        for(Instruction i : assembly.instructions) {
            joiner.add(i.data());
        }
        outStream.writeBytes(joiner.toString());
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
