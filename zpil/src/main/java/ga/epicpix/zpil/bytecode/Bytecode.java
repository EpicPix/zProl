package ga.epicpix.zpil.bytecode;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zpil.exceptions.RedefinedInstructionException;
import ga.epicpix.zpil.exceptions.UnknownInstructionException;
import ga.epicpix.zpil.StringTable;
import ga.epicpix.zprol.structures.*;
import ga.epicpix.zprol.structures.Class;

import java.io.*;
import java.util.ArrayList;
import java.util.Objects;

public final class Bytecode implements IBytecode {

    public static final IBytecode BYTECODE = new Bytecode();

    /* package-private */ static class BytecodeInstructionData {
        public final int id;
        public final String name;
        public final BytecodeValueType[] values;
        public final IBytecodeInstructionGenerator generator;

        public BytecodeInstructionData(int id, String name, BytecodeValueType[] values, IBytecodeInstructionGenerator generator) {
            this.id = id;
            this.name = name;
            this.values = values;
            this.generator = generator;
        }
    }

    private final ArrayList<BytecodeInstructionData> data = new ArrayList<>();

    private Bytecode() {
        registerSizedInstruction(0, "return", new int[] {0, 1, 2, 4, 8});
        registerSizedInstruction(5, "store_local", new int[] {1, 2, 4, 8}, BytecodeValueType.SHORT);
        registerInstruction(9, "invoke", BytecodeValueType.FUNCTION);
        registerInstruction(10, "class_field_load", BytecodeValueType.CLASS, BytecodeValueType.STRING);
        registerInstruction(11, "apop");
        registerInstruction(12, "areturn");
        registerInstruction(13, "aload_local", BytecodeValueType.SHORT);
        registerInstruction(14, "astore_local", BytecodeValueType.SHORT);
        registerInstruction(15, "adup");
        registerSizedInstruction(16, "pop", new int[] {1, 2, 4, 8});
        registerSizedInstruction(20, "load_local", new int[] {1, 2, 4, 8}, BytecodeValueType.SHORT);
        registerSizedInstruction(24, "cast" + getInstructionPrefix(1), new int[] {2, 4, 8});
        registerSizedInstruction(27, "cast" + getInstructionPrefix(2), new int[] {1, 4, 8});
        registerSizedInstruction(30, "cast" + getInstructionPrefix(4), new int[] {1, 2, 8});
        registerSizedInstruction(33, "cast" + getInstructionPrefix(8), new int[] {1, 2, 4});
        registerInstruction(36, getInstructionPrefix(1) + "push", BytecodeValueType.BYTE);
        registerInstruction(37, getInstructionPrefix(2) + "push", BytecodeValueType.SHORT);
        registerInstruction(38, getInstructionPrefix(4) + "push", BytecodeValueType.INT);
        registerInstruction(39, getInstructionPrefix(8) + "push", BytecodeValueType.LONG);
        registerSizedInstruction(40, "eq", new int[] {1, 2, 4, 8});
        registerSizedInstruction(44, "neq", new int[] {1, 2, 4, 8});
        registerSizedInstruction(48, "add", new int[] {1, 2, 4, 8});
        registerSizedInstruction(52, "sub", new int[] {1, 2, 4, 8});
        registerSizedInstruction(56, "mul", new int[] {1, 2, 4, 8});
        registerSizedInstruction(60, "mulu", new int[] {1, 2, 4, 8});
        registerSizedInstruction(64, "div", new int[] {1, 2, 4, 8});
        registerSizedInstruction(68, "divu", new int[] {1, 2, 4, 8});
        registerSizedInstruction(72, "mod", new int[] {1, 2, 4, 8});
        registerSizedInstruction(76, "modu", new int[] {1, 2, 4, 8});
        registerSizedInstruction(80, "and", new int[] {1, 2, 4, 8});
        registerSizedInstruction(84, "shift_left", new int[] {1, 2, 4, 8});
        registerSizedInstruction(88, "shift_right", new int[] {1, 2, 4, 8});
        registerInstruction(92, "push_string", BytecodeValueType.STRING);
        registerSizedInstruction(93, "dup", new int[] {1, 2, 4, 8});
        registerSizedInstruction(97, "or", new int[] {1, 2, 4, 8});
        registerInstruction(101, "class_field_store", BytecodeValueType.CLASS, BytecodeValueType.STRING);
        registerInstruction(102, "jmp", BytecodeValueType.SHORT);
        registerInstruction(103, "eqjmp", BytecodeValueType.SHORT);
        registerInstruction(104, "neqjmp", BytecodeValueType.SHORT);
        registerSizedInstruction(105, "load_array", new int[] {1, 2, 4, 8});
        registerInstruction(109, "aload_array");
        registerSizedInstruction(110, "store_array", new int[] {1, 2, 4, 8});
        registerInstruction(114, "astore_array");
        registerSizedInstruction(115, "load_field", new int[] {1, 2, 4, 8}, BytecodeValueType.FIELD);
        registerInstruction(119, "aload_field", BytecodeValueType.FIELD);
        registerSizedInstruction(120, "store_field", new int[] {1, 2, 4, 8}, BytecodeValueType.FIELD);
        registerInstruction(124, "astore_field", BytecodeValueType.FIELD);
        registerInstruction(125, "aeq");
        registerInstruction(126, "aneq");
        registerSizedInstruction(127, "lt", new int[] {1, 2, 4, 8});
        registerSizedInstruction(131, "ltu", new int[] {1, 2, 4, 8});
        registerSizedInstruction(135, "le", new int[] {1, 2, 4, 8});
        registerSizedInstruction(139, "leu", new int[] {1, 2, 4, 8});
        registerSizedInstruction(143, "gt", new int[] {1, 2, 4, 8});
        registerSizedInstruction(147, "gtu", new int[] {1, 2, 4, 8});
        registerSizedInstruction(151, "ge", new int[] {1, 2, 4, 8});
        registerSizedInstruction(155, "geu", new int[] {1, 2, 4, 8});
        registerInstruction(159, "invoke_class", BytecodeValueType.METHOD);
        registerInstruction(160, "push_false");
        registerInstruction(161, "push_true");
        registerInstruction(162, "null");
        registerSizedInstruction(163, "neg", new int[] {1, 2, 4, 8});

        registerInstruction(-1, "int"); // internal usage, should not be in zpil files
    }

    private void registerSizedInstruction(int id, String name, int[] sizes, BytecodeValueType... values) {
        for(int i = 0; i<sizes.length; i++) {
            registerInstruction(id + i, getInstructionPrefix(sizes[i]) + name, values);
        }
    }

    private void registerInstruction(int id, String name, BytecodeValueType... values) {
        for(BytecodeInstructionData d : data) {
            if(d.id == id) {
                throw new RedefinedInstructionException("Instruction with id '" + d.id + "' already registered with name '" + d.name + "'");
            }else if(d.name.equals(name)) {
                throw new RedefinedInstructionException("Instruction with name '" + d.name + "' already registered with id '" + d.id + "'");
            }
        }
        data.add(new BytecodeInstructionData(id, name, values, (args) -> {
            for(BytecodeInstructionData d : data) {
                if(d.id == id) {
                    if(d.values.length != args.length) throw new IllegalArgumentException("Invalid amount of arguments (" + args.length + "), expected " + d.values.length);

                    return new BytecodeInstruction(d, args);
                }
            }
            throw new UnknownInstructionException("Data about instruction not found (" + id + ")");
        }));
    }

    public String getInstructionPrefix(int size) {
        switch(size) {
            case 0: return "v";
            case 1: return "b";
            case 2: return "s";
            case 4: return "i";
            case 8: return "l";
            case 16: return "h";
            default: throw new IllegalArgumentException("Instruction prefix with size " + size + " does not exist");
        }
    }

    public IBytecodeInstructionGenerator getInstruction(int id) {
        for(BytecodeInstructionData d : data) {
            if(d.id == id) {
                return d.generator;
            }
        }
        throw new UnknownInstructionException("Instruction with id '" + id + "' not found");
    }

    public BytecodeValueType[] getInstructionValueTypesRequirements(int id) {
        for(BytecodeInstructionData d : data) {
            if(d.id == id) {
                BytecodeValueType[] clone = new BytecodeValueType[d.values.length];
                System.arraycopy(d.values, 0, clone, 0, d.values.length);
                return clone;
            }
        }
        throw new UnknownInstructionException("Instruction with id '" + id + "' not found");
    }

    public IBytecodeInstructionGenerator getInstruction(String name) {
        for(BytecodeInstructionData d : data) {
            if(d.name.equals(name)) {
                return d.generator;
            }
        }
        throw new UnknownInstructionException("Instruction with name '" + name + "' not found");
    }

    public IBytecodeStorage createStorage() {
        return new BytecodeData();
    }

    public static void write(IBytecodeInstruction instr, GeneratedData data, DataOutputStream out) throws IOException {
        out.writeByte(instr.getId());
        BytecodeValueType[] values = Bytecode.BYTECODE.getInstructionValueTypesRequirements(instr.getId());
        Object[] args = instr.getData();
        for(int i = 0; i<values.length; i++) {
            BytecodeValueType type = values[i];
            Object val = args[i];
            if(type == BytecodeValueType.STRING) {
                if(val instanceof String) val = data.stringTable.getStringIndex((String) val);
                else throw new IllegalArgumentException(val.toString().getClass().getName());
            }else if(type == BytecodeValueType.FUNCTION) {
                if(val instanceof Function) {
                    Function v = (Function) val;
                    ArrayList<Function> functions = data.functions;
                    for(int j = 0; j < functions.size(); j++) {
                        Function f = functions.get(j);
                        if(!Objects.equals(f.namespace, v.namespace)) continue;
                        if(!f.name.equals(v.name)) continue;
                        if(!f.signature.equals(v.signature)) continue;
                        val = j;
                        break;
                    }
                }
                else throw new IllegalArgumentException(val.toString().getClass().getName());
            }else if(type == BytecodeValueType.CLASS) {
                if(val instanceof Class) {
                    Class v = (Class) val;
                    ArrayList<Class> classes = data.classes;
                    for(int j = 0; j < classes.size(); j++) {
                        Class f = classes.get(j);
                        if(!Objects.equals(f.namespace, v.namespace)) continue;
                        if(!f.name.equals(v.name)) continue;
                        val = j;
                        break;
                    }
                }
                else throw new IllegalArgumentException(val.toString().getClass().getName());
            }else if(type == BytecodeValueType.FIELD) {
                if(val instanceof Field) {
                    Field v = (Field) val;
                    ArrayList<Field> fields = data.fields;
                    for(int j = 0; j < fields.size(); j++) {
                        Field f = fields.get(j);
                        if(!Objects.equals(f.namespace, v.namespace)) continue;
                        if(!f.name.equals(v.name)) continue;
                        if(!f.type.equals(v.type)) continue;
                        val = j;
                        break;
                    }
                }
                else throw new IllegalArgumentException(val.toString().getClass().getName());
            }else if(type == BytecodeValueType.METHOD) {
                if(val instanceof Method) {
                    Method v = (Method) val;
                    ArrayList<Class> classes = data.classes;
                    out: for(int j = 0; j < classes.size(); j++) {
                        Class f = classes.get(j);
                        if(!Objects.equals(f.namespace, v.namespace)) continue;
                        for(int k = 0; k<f.methods.length; k++) {
                            Method m = f.methods[k];
                            if(!m.name.equals(v.name)) continue;
                            if(!m.signature.equals(v.signature)) continue;
                            val = ((long) k << 32) | j;
                            break out;
                        }
                    }
                }
                else throw new IllegalArgumentException(val.getClass().getName());
            }
            if(!(val instanceof Number)) {
                throw new IllegalArgumentException(val.getClass().getName());
            }
            Number num = (Number) val;
            if(type.getSize() == 1) {
                out.writeByte(num.byteValue());
            } else if(type.getSize() == 2) {
                out.writeShort(num.shortValue());
            } else if(type.getSize() == 4) {
                out.writeInt(num.intValue());
            } else if(type.getSize() == 8) {
                out.writeLong(num.longValue());
            } else {
                throw new IllegalStateException("Invalid size of bytecode type: " + type.getSize());
            }
        }
    }

    public static IBytecodeInstruction read(DataInput input) throws IOException {
        int id = input.readUnsignedByte();
        IBytecodeInstructionGenerator instructionGenerator = Bytecode.BYTECODE.getInstruction(id);
        BytecodeValueType[] values = Bytecode.BYTECODE.getInstructionValueTypesRequirements(id);
        Object[] args = new Object[values.length];
        for(int i = 0; i<values.length; i++) {
            BytecodeValueType type = values[i];
            if(type.getSize() == 1) {
                args[i] = input.readByte();
            } else if(type.getSize() == 2) {
                args[i] = input.readShort();
            } else if(type.getSize() == 4) {
                args[i] = input.readInt();
            } else if(type.getSize() == 8) {
                args[i] = input.readLong();
            } else {
                throw new IllegalStateException("Invalid size of bytecode type: " + type.getSize());
            }
            ;
            if(type == BytecodeValueType.STRING || type == BytecodeValueType.FUNCTION || type == BytecodeValueType.CLASS || type == BytecodeValueType.FIELD) {
                args[i] = ((Number) args[i]).intValue();
            } else if(type == BytecodeValueType.METHOD) {
                args[i] = ((Number) args[i]).longValue();
            }
        }
        return instructionGenerator.createInstruction(args);
    }

    public static void postRead(IBytecodeInstruction instr, GeneratedData data) {
        BytecodeValueType[] values = Bytecode.BYTECODE.getInstructionValueTypesRequirements(instr.getId());
        for(int i = 0; i<values.length; i++) {
            if(values[i] == BytecodeValueType.STRING) {
                int index = ((Number) instr.getData()[i]).intValue();
                instr.getData()[i] = data.stringTable.getString(index);
            }else if(values[i] == BytecodeValueType.FUNCTION) {
                int index = ((Number) instr.getData()[i]).intValue();
                instr.getData()[i] = data.functions.get(index);
            }else if(values[i] == BytecodeValueType.CLASS) {
                int index = ((Number) instr.getData()[i]).intValue();
                instr.getData()[i] = data.classes.get(index);
            }else if(values[i] == BytecodeValueType.FIELD) {
                int index = ((Number) instr.getData()[i]).intValue();
                instr.getData()[i] = data.fields.get(index);
            }else if(values[i] == BytecodeValueType.METHOD) {
                long index = ((Number) instr.getData()[i]).longValue();
                instr.getData()[i] = data.classes.get((int) (index)).methods[(int) (index >> 32)];
            }
        }
    }

    public static void prepareConstantPool(IBytecodeInstruction instr, StringTable pool) {
        BytecodeValueType[] values = Bytecode.BYTECODE.getInstructionValueTypesRequirements(instr.getId());
        for(int i = 0; i<values.length; i++) {
            BytecodeValueType type = values[i];
            Object val = instr.getData()[i];
            if(type == BytecodeValueType.STRING) {
                if(val instanceof String) pool.getOrCreateStringIndex((String) val);
                else throw new IllegalArgumentException(val.getClass().getName());
            }else if(type == BytecodeValueType.FUNCTION) {
                if(val instanceof Function) {
                    Function v = (Function) val;
                    pool.getOrCreateStringIndex(v.namespace);
                    pool.getOrCreateStringIndex(v.name);
                    pool.getOrCreateStringIndex(v.signature.toString());
                }
                else throw new IllegalArgumentException(val.getClass().getName());
            }else if(type == BytecodeValueType.CLASS) {
                if(val instanceof Class) {
                    Class v = (Class) val;
                    pool.getOrCreateStringIndex(v.namespace);
                    pool.getOrCreateStringIndex(v.name);
                }
                else throw new IllegalArgumentException(val.getClass().getName());
            }else if(type == BytecodeValueType.METHOD) {
                if(val instanceof Method) {
                    Method v = (Method) val;
                    pool.getOrCreateStringIndex(v.namespace);
                    pool.getOrCreateStringIndex(v.name);
                    pool.getOrCreateStringIndex(v.className);
                    pool.getOrCreateStringIndex(v.signature.toString());
                }
                else throw new IllegalArgumentException(val.getClass().getName());
            }else if(type == BytecodeValueType.FIELD) {
                if(val instanceof Field) {
                    Field v = (Field) val;
                    pool.getOrCreateStringIndex(v.namespace);
                    pool.getOrCreateStringIndex(v.name);
                    pool.getOrCreateStringIndex(v.type.toString());
                }
                else throw new IllegalArgumentException(val.getClass().getName());
            }
        }
    }

}
