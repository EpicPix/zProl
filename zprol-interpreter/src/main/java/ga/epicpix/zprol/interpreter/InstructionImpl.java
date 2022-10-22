package ga.epicpix.zprol.interpreter;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.interpreter.classes.ClassImpl;
import ga.epicpix.zprol.interpreter.classes.StringImpl;
import ga.epicpix.zprol.structures.*;
import ga.epicpix.zprol.structures.Class;
import ga.epicpix.zprol.types.PrimitiveType;

import java.util.Objects;

class InstructionImpl {

    static void runInstruction(GeneratedData file, VMState state, IBytecodeInstruction instruction, LocalStorage locals) {
        switch(instruction.getName()) {
            case "vreturn":
                state.returnFunction(null, 0);
                break;
            case "areturn":
            case "lreturn":
                state.returnFunction(state.stack.pop(8).value, 8);
                break;
            case "push_string":
                state.stack.push(new StringImpl((String) instruction.getData()[0]), 8);
                break;
            case "invoke":
                Interpreter.runFunction(file, state, (Function) instruction.getData()[0]);
                break;
            case "invoke_class":
                Interpreter.runMethod(file, state, (Method) instruction.getData()[0]);
                break;
            case "icastl":
                state.stack.push((long) (Integer) state.stack.pop(4).value, 8);
                break;
            case "bcasti":
                state.stack.push((int) (Byte) state.stack.pop(1).value, 4);
                break;
            case "bpush":
                state.stack.push((Byte) instruction.getData()[0], 1);
                break;
            case "ipush":
                state.stack.push((Integer) instruction.getData()[0], 4);
                break;
            case "lpush":
                state.stack.push((Long) instruction.getData()[0], 8);
                break;
            case "null":
            case "push_false":
                state.stack.push(0L, 8);
                break;
            case "push_true":
                state.stack.push(1L, 8);
                break;
            case "iadd":
                state.stack.push((Integer) state.stack.pop(4).value + (Integer) state.stack.pop(4).value, 4);
                break;
            case "ladd":
                state.stack.push((Long) state.stack.pop(8).value + (Long) state.stack.pop(8).value, 8);
                break;
            case "lmul":
            case "lmulu":
                state.stack.push((Long) state.stack.pop(8).value * (Long) state.stack.pop(8).value, 8);
                break;
            case "band":
                state.stack.push((byte) ((Byte) state.stack.pop(1).value & (Byte) state.stack.pop(1).value), 1);
                break;
            case "iand":
                state.stack.push((Integer) state.stack.pop(4).value & (Integer) state.stack.pop(4).value, 4);
                break;
            case "land":
                state.stack.push((Long) state.stack.pop(8).value & (Long) state.stack.pop(8).value, 8);
                break;
            case "lsub": {
                Long b = (Long) state.stack.pop(8).value;
                Long a = (Long) state.stack.pop(8).value;
                state.stack.push(a - b, 8);
                break;
            }
            case "bor":
                state.stack.push((byte) ((Byte) state.stack.pop(1).value | (Byte) state.stack.pop(1).value), 1);
                break;
            case "lor":
                state.stack.push((Long) state.stack.pop(8).value | (Long) state.stack.pop(8).value, 8);
                break;
            case "lleu": {
                Long b = (Long) state.stack.pop(8).value;
                Long a = (Long) state.stack.pop(8).value;
                state.stack.push(Long.compareUnsigned(a, b) <= 0 ? 1L : 0L, 8);
                break;
            }
            case "lltu": {
                Long b = (Long) state.stack.pop(8).value;
                Long a = (Long) state.stack.pop(8).value;
                state.stack.push(Long.compareUnsigned(a, b) < 0 ? 1L : 0L, 8);
                break;
            }
            case "lgeu": {
                Long b = (Long) state.stack.pop(8).value;
                Long a = (Long) state.stack.pop(8).value;
                state.stack.push(Long.compareUnsigned(a, b) >= 0 ? 1L : 0L, 8);
                break;
            }
            case "lgtu": {
                Long b = (Long) state.stack.pop(8).value;
                Long a = (Long) state.stack.pop(8).value;
                state.stack.push(Long.compareUnsigned(a, b) > 0 ? 1L : 0L, 8);
                break;
            }
            case "ieq": {
                Integer b = (Integer) state.stack.pop(4).value;
                Integer a = (Integer) state.stack.pop(4).value;
                state.stack.push(Objects.equals(a, b) ? 1L : 0L, 8);
                break;
            }
            case "aeq":
            case "leq": {
                Object b = state.stack.pop(8).value;
                Object a = state.stack.pop(8).value;
                state.stack.push(Objects.equals(a, b) ? 1L : 0L, 8);
                break;
            }
            case "ineq": {
                Integer b = (Integer) state.stack.pop(4).value;
                Integer a = (Integer) state.stack.pop(4).value;
                state.stack.push(!Objects.equals(a, b) ? 1L : 0L, 8);
                break;
            }
            case "aneq":
            case "lneq": {
                Object b = state.stack.pop(8).value;
                Object a = state.stack.pop(8).value;
                state.stack.push(!Objects.equals(a, b) ? 1L : 0L, 8);
                break;
            }
            case "neqjmp":
                if((Long) state.stack.pop(8).value == 0) {
                    state.currentInstruction += (Short) instruction.getData()[0] - 1;
                }
                break;
            case "jmp":
                state.currentInstruction += (Short) instruction.getData()[0] - 1;
                break;
            case "apop":
            case "lpop":
                state.stack.pop(8);
                break;
            case "aload_field":
            case "lload_field":
                state.stack.push(state.getFieldValue((Field) instruction.getData()[0]), 8);
                break;
            case "astore_field": {
                FieldStorage f = state.getField((Field) instruction.getData()[0]);
                if(f.field.modifiers.contains(FieldModifiers.CONST) && f.defined) {
                    throw new RuntimeException("Cannot redefine a constant field");
                }
                f.value = state.stack.pop(8).value;
                f.defined = true;
                break;
            }
            case "lload_local":
            case "aload_local":
                state.stack.push(locals.get((Short) instruction.getData()[0], 8).value, 8);
                break;
            case "astore_local":
            case "lstore_local":
                locals.set(state.stack.pop(8).value, (Short) instruction.getData()[0], 8);
                break;
            case "lstore_array": {
                Object index = state.stack.pop(8).value;
                Object pointer = state.stack.pop(8).value;
                Object value = state.stack.pop(8).value;
                if(!(pointer instanceof Long)) {
                    throw new RuntimeException("The array must be a pointer");
                }
                Long lp = (Long) pointer;
                if(!(index instanceof Long)) {
                    throw new RuntimeException("The index must be a long");
                }
                Long idx = (Long) index;
                if(!(value instanceof Long)) {
                    if(value instanceof ILocatable) {
                        value = state.memory.objectToPointer((ILocatable) value);
                    }else {
                        throw new NotImplementedException("Cannot store objects in arrays yet");
                    }
                }
                Long val = (Long) value;
                state.memory.setLong(lp + idx * 8, val);
                break;
            }
            case "lload_array": {
                Object index = state.stack.pop(8).value;
                Object pointer = state.stack.pop(8).value;
                if(!(pointer instanceof Long)) {
                    throw new RuntimeException("The array must be a pointer");
                }
                Long lp = (Long) pointer;
                if(!(index instanceof Long)) {
                    throw new RuntimeException("The index must be a long");
                }
                Long idx = (Long) index;
                state.stack.push(state.memory.getLong(lp + idx * 8), 8);
                break;
            }
            case "class_field_load": {
                Object val = state.stack.pop(8).value;
                Class clz = (Class) instruction.getData()[0];
                String fname = (String) instruction.getData()[1];
                if(val instanceof Long) {
                    long l = (Long) val;
                    if((l & 0x8000000000000000L) != 0) {
                        val = state.memory.pointerToObject(l);
                    }
                }
                if(val instanceof ClassImpl) {
                    ClassImpl ci = (ClassImpl) val;
                    ClassField fi = null;
                    for(ClassField c : clz.fields) {
                        if(c.name.equals(fname)) {
                            fi = c;
                            break;
                        }
                    }
                    if(fi == null) {
                        throw new RuntimeException("Unknown field '" + fname + "' in " + clz.namespace + "." + clz.name);
                    }
                    Object value = ci.getFieldValue(file, state, fname);
                    if(value instanceof Byte) {
                        if(fi.type instanceof PrimitiveType) {
                            PrimitiveType t = (PrimitiveType) fi.type;
                            if(t.size != 1) {
                                throw new IllegalArgumentException("Field type and return value don't match");
                            }
                        } else {
                            throw new IllegalArgumentException("Field type and return value don't match");
                        }
                        state.stack.push(value, 1);
                    } else if(value instanceof Short) {
                        if(fi.type instanceof PrimitiveType) {
                            PrimitiveType t = (PrimitiveType) fi.type;
                            if(t.size != 2) {
                                throw new IllegalArgumentException("Field type and return value don't match");
                            }
                        } else {
                            throw new IllegalArgumentException("Field type and return value don't match");
                        }
                        state.stack.push(value, 2);
                    } else if(value instanceof Integer) {
                        if(fi.type instanceof PrimitiveType) {
                            PrimitiveType t = (PrimitiveType) fi.type;
                            if(t.size != 4) {
                                throw new IllegalArgumentException("Field type and return value don't match");
                            }
                        } else {
                            throw new IllegalArgumentException("Field type and return value don't match");
                        }
                        state.stack.push(value, 4);
                    } else state.stack.push(value, 8);
                } else if(val instanceof Long) {
                    Long l = (Long) val;
                    int byteIndex = 0;
                    ClassField cf = null;
                    for(ClassField c : clz.fields) {
                        if(c.name.equals(fname)) {
                            cf = c;
                            break;
                        }
                        if(c.type instanceof PrimitiveType) {
                            PrimitiveType p = (PrimitiveType) c.type;
                            byteIndex += p.size;
                        } else byteIndex += 8;
                    }
                    if(cf == null)
                        throw new RuntimeException("Unknown field '" + fname + "' in " + clz.namespace + "." + clz.name);

                    if(cf.type instanceof PrimitiveType) {
                        PrimitiveType p = (PrimitiveType) cf.type;
                        if(p.size == 1) state.stack.push(state.memory.get(l + byteIndex), 1);
                        else if(p.size == 2) state.stack.push(state.memory.getShort(l + byteIndex), 2);
                        else if(p.size == 4) state.stack.push(state.memory.getInt(l + byteIndex), 4);
                        else if(p.size == 8) state.stack.push(state.memory.getLong(l + byteIndex), 8);
                    } else {
                        state.stack.push(state.memory.getLong(l + byteIndex), 8);
                    }
                } else {
                    throw new IllegalStateException("Expected a class type or a pointer, got " + val.getClass().getSimpleName() + " (" + val + ")");
                }
                break;
            }
            case "class_field_store": {
                Object loc = state.stack.pop(8).value;
                Class clz = (Class) instruction.getData()[0];
                String fname = (String) instruction.getData()[1];
                int byteIndex = 0;
                ClassField cf = null;
                for(ClassField c : clz.fields) {
                    if(c.name.equals(fname)) {
                        cf = c;
                        break;
                    }
                    if(c.type instanceof PrimitiveType) {
                        PrimitiveType p = (PrimitiveType) c.type;
                        byteIndex += p.size;
                    } else byteIndex += 8;
                }
                if(cf == null)
                    throw new RuntimeException("Unknown field '" + fname + "' in " + clz.namespace + "." + clz.name);
                Object data;
                if(cf.type instanceof PrimitiveType) {
                    PrimitiveType pt = (PrimitiveType) cf.type;
                    data = state.stack.pop(pt.size).value;
                } else data = state.stack.pop(8).value;
                if(loc instanceof Long) {
                    Long l = (Long) loc;
                    if(cf.type instanceof PrimitiveType) {
                        PrimitiveType p = (PrimitiveType) cf.type;
                        if(p.size == 1) state.memory.set(l + byteIndex, (Byte) data);
                        else if(p.size == 2) state.memory.setShort(l + byteIndex, (Short) data);
                        else if(p.size == 4) state.memory.setInt(l + byteIndex, (Integer) data);
                        else if(p.size == 8) state.memory.setLong(l + byteIndex, (Long) data);
                    } else {
                        if(data instanceof ILocatable) {
                            data = state.memory.objectToPointer((ILocatable) data);
                        }
                        if(data instanceof Long) {
                            state.memory.setLong(l + byteIndex, (Long) data);
                        } else {
                            throw new NotImplementedException("This value cannot be stored: 0x" + Long.toHexString(l) + " / " + cf.type + " / " + data);
                        }
                    }
                } else if(loc instanceof ClassImpl) {
                    ClassImpl ci = (ClassImpl) loc;
                    ci.setFieldValue(file, state, fname, data);
                } else {
                    throw new IllegalArgumentException("Expected a pointer or ClassImpl");
                }
                break;
            }
            default:
                throw new NotImplementedException("Cannot handle instruction {" + instruction + "}");
        }
    }

}
