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
            case "vreturn" -> state.returnFunction(null, 0);
            case "areturn", "lreturn" -> state.returnFunction(state.stack.pop(8).value(), 8);
            case "push_string" -> state.stack.push(new StringImpl((String) instruction.getData()[0]), 8);
            case "invoke" -> Interpreter.runFunction(file, state, (Function) instruction.getData()[0]);
            case "invoke_class" -> Interpreter.runMethod(file, state, (Method) instruction.getData()[0]);
            case "icastl" -> state.stack.push((long) (Integer) state.stack.pop(4).value(), 8);
            case "bcasti" -> state.stack.push((int) (Byte) state.stack.pop(1).value(), 4);
            case "bpush" -> state.stack.push((Byte) instruction.getData()[0], 1);
            case "ipush" -> state.stack.push((Integer) instruction.getData()[0], 4);
            case "lpush" -> state.stack.push((Long) instruction.getData()[0], 8);
            case "null", "push_false" -> state.stack.push(0L, 8);
            case "push_true" -> state.stack.push(1L, 8);
            case "iadd" -> state.stack.push((Integer) state.stack.pop(4).value() + (Integer) state.stack.pop(4).value(), 4);
            case "ladd" -> state.stack.push((Long) state.stack.pop(8).value() + (Long) state.stack.pop(8).value(), 8);
            case "lmul", "lmulu" -> state.stack.push((Long) state.stack.pop(8).value() * (Long) state.stack.pop(8).value(), 8);
            case "band" -> state.stack.push((byte) ((Byte) state.stack.pop(1).value() & (Byte) state.stack.pop(1).value()), 1);
            case "iand" -> state.stack.push((Integer) state.stack.pop(4).value() & (Integer) state.stack.pop(4).value(), 4);
            case "land" -> state.stack.push((Long) state.stack.pop(8).value() & (Long) state.stack.pop(8).value(), 8);
            case "lsub" -> {
                var b = (Long) state.stack.pop(8).value();
                var a = (Long) state.stack.pop(8).value();
                state.stack.push(a - b, 8);
            }
            case "bor" -> state.stack.push((byte) ((Byte) state.stack.pop(1).value() | (Byte) state.stack.pop(1).value()), 1);
            case "lor" -> state.stack.push((Long) state.stack.pop(8).value() | (Long) state.stack.pop(8).value(), 8);
            case "lleu" -> {
                var b = (Long) state.stack.pop(8).value();
                var a = (Long) state.stack.pop(8).value();
                state.stack.push(Long.compareUnsigned(a, b) <= 0 ? 1L : 0L, 8);
            }
            case "lltu" -> {
                var b = (Long) state.stack.pop(8).value();
                var a = (Long) state.stack.pop(8).value();
                state.stack.push(Long.compareUnsigned(a, b) < 0 ? 1L : 0L, 8);
            }
            case "lgeu" -> {
                var b = (Long) state.stack.pop(8).value();
                var a = (Long) state.stack.pop(8).value();
                state.stack.push(Long.compareUnsigned(a, b) >= 0 ? 1L : 0L, 8);
            }
            case "lgtu" -> {
                var b = (Long) state.stack.pop(8).value();
                var a = (Long) state.stack.pop(8).value();
                state.stack.push(Long.compareUnsigned(a, b) > 0 ? 1L : 0L, 8);
            }
            case "ieq" -> {
                var b = state.stack.pop(4).value();
                var a = state.stack.pop(4).value();
                state.stack.push(Objects.equals(a, b) ? 1L : 0L, 8);
            }
            case "aeq", "leq" -> {
                var b = state.stack.pop(8).value();
                var a = state.stack.pop(8).value();
                state.stack.push(Objects.equals(a, b) ? 1L : 0L, 8);
            }
            case "ineq" -> {
                var b = state.stack.pop(4).value();
                var a = state.stack.pop(4).value();
                state.stack.push(!Objects.equals(a, b) ? 1L : 0L, 8);
            }
            case "aneq", "lneq" -> {
                var b = state.stack.pop(8).value();
                var a = state.stack.pop(8).value();
                state.stack.push(!Objects.equals(a, b) ? 1L : 0L, 8);
            }
            case "neqjmp" -> {
                if((Long) state.stack.pop(8).value() == 0) {
                    state.currentInstruction += (Short) instruction.getData()[0] - 1;
                }
            }
            case "jmp" -> state.currentInstruction += (Short) instruction.getData()[0] - 1;
            case "apop", "lpop" -> state.stack.pop(8);
            case "aload_field", "lload_field" -> state.stack.push(state.getFieldValue((Field) instruction.getData()[0]), 8);
            case "astore_field" -> {
                var f = state.getField((Field) instruction.getData()[0]);
                if(f.field.modifiers.contains(FieldModifiers.CONST) && f.defined) {
                    throw new RuntimeException("Cannot redefine a constant field");
                }
                f.value = state.stack.pop(8).value();
                f.defined = true;
            }
            case "lload_local", "aload_local" -> state.stack.push(locals.get((Short) instruction.getData()[0], 8).value(), 8);
            case "astore_local", "lstore_local" -> locals.set(state.stack.pop(8).value(), (Short) instruction.getData()[0], 8);
            case "lstore_array" -> {
                var index = state.stack.pop(8).value();
                var pointer = state.stack.pop(8).value();
                var value = state.stack.pop(8).value();
                if(!(pointer instanceof Long lp)) {
                    throw new RuntimeException("The array must be a pointer");
                }
                if(!(index instanceof Long idx)) {
                    throw new RuntimeException("The index must be a long");
                }
                if(!(value instanceof Long val)) {
                    throw new NotImplementedException("Cannot store objects in arrays yet");
                }
                state.memory.setLong(lp + idx * 8, val);
            }
            case "lload_array" -> {
                var index = state.stack.pop(8).value();
                var pointer = state.stack.pop(8).value();
                if(!(pointer instanceof Long lp)) {
                    throw new RuntimeException("The array must be a pointer");
                }
                if(!(index instanceof Long idx)) {
                    throw new RuntimeException("The index must be a long");
                }
                state.stack.push(state.memory.getLong(lp + idx * 8), 8);
            }
            case "class_field_load" -> {
                var val = state.stack.pop(8).value();
                var clz = (Class) instruction.getData()[0];
                var fname = (String) instruction.getData()[1];
                if(val instanceof ClassImpl ci) {
                    ClassField fi = null;
                    for(var c : clz.fields) {
                        if(c.name.equals(fname)) {
                            fi = c;
                            break;
                        }
                    }
                    if(fi == null) {
                        throw new RuntimeException("Unknown field '" + fname + "' in " + clz.namespace + "." + clz.name);
                    }
                    var value = ci.getFieldValue(file, state, fname);
                    if(value instanceof Byte) {
                        if(!(fi.type instanceof PrimitiveType t && t.size == 1)) {
                            throw new IllegalArgumentException("Field type and return value don't match");
                        }
                        state.stack.push(value, 1);
                    }
                    else if(value instanceof Short) {
                        if(!(fi.type instanceof PrimitiveType t && t.size == 2)) {
                            throw new IllegalArgumentException("Field type and return value don't match");
                        }
                        state.stack.push(value, 2);
                    }
                    else if(value instanceof Integer) {
                        if(!(fi.type instanceof PrimitiveType t && t.size == 4)) {
                            throw new IllegalArgumentException("Field type and return value don't match");
                        }
                        state.stack.push(value, 4);
                    }
                    else state.stack.push(value, 8);
                }else if(val instanceof Long l) {
                    int byteIndex = 0;
                    ClassField cf = null;
                    for(var c : clz.fields) {
                        if(c.name.equals(fname)) {
                            cf = c;
                            break;
                        }
                        byteIndex += c.type instanceof PrimitiveType p ? p.size : 8;
                    }
                    if(cf == null) throw new RuntimeException("Unknown field '" + fname + "' in " + clz.namespace + "." + clz.name);

                    if(cf.type instanceof PrimitiveType p) {
                        if(p.size == 1) state.stack.push(state.memory.get(l + byteIndex), 1);
                        else if(p.size == 2) state.stack.push(state.memory.getShort(l + byteIndex), 2);
                        else if(p.size == 4) state.stack.push(state.memory.getInt(l + byteIndex), 4);
                        else if(p.size == 8) state.stack.push(state.memory.getLong(l + byteIndex), 8);
                    }else {
                        state.stack.push(state.memory.getLong(l + byteIndex), 8);
                    }
                }else {
                    throw new IllegalStateException("Expected a class type or a pointer, got " + val.getClass().getSimpleName() + " (" + val + ")");
                }
            }
            case "class_field_store" -> {
                var loc = state.stack.pop(8).value();
                var clz = (Class) instruction.getData()[0];
                var fname = (String) instruction.getData()[1];
                int byteIndex = 0;
                ClassField cf = null;
                for(var c : clz.fields) {
                    if(c.name.equals(fname)) {
                        cf = c;
                        break;
                    }
                    byteIndex += c.type instanceof PrimitiveType p ? p.size : 8;
                }
                if(cf == null) throw new RuntimeException("Unknown field '" + fname + "' in " + clz.namespace + "." + clz.name);
                Object data = state.stack.pop(cf.type instanceof PrimitiveType pt ? pt.size : 8).value();
                if(loc instanceof Long l) {
                    if(cf.type instanceof PrimitiveType p) {
                        if(p.size == 1) state.memory.set(l + byteIndex, (Byte) data);
                        else if(p.size == 2) state.memory.setShort(l + byteIndex, (Short) data);
                        else if(p.size == 4) state.memory.setInt(l + byteIndex, (Integer) data);
                        else if(p.size == 8) state.memory.setLong(l + byteIndex, (Long) data);
                    }else if(data instanceof Long) {
                        state.memory.setLong(l + byteIndex, (Long) data);
                    }else {
                        throw new NotImplementedException("Cannot store objects in classes yet: " + loc + " / " + cf.type);
                    }
                }else if(loc instanceof ClassImpl ci) {
                    ci.setFieldValue(file, state, fname, data);
                }else {
                    throw new IllegalArgumentException("Expected a pointer or ClassImpl");
                }
            }
            default -> throw new NotImplementedException("Cannot handle instruction {" + instruction + "}");
        }
    }

}
