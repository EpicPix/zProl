package ga.epicpix.zprol.interpreter;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.interpreter.classes.ClassImpl;
import ga.epicpix.zprol.interpreter.classes.StringImpl;
import ga.epicpix.zprol.structures.*;
import ga.epicpix.zprol.structures.Class;
import ga.epicpix.zprol.types.PrimitiveType;

class InstructionImpl {

    static void runInstruction(GeneratedData file, VMState state, IBytecodeInstruction instruction, LocalStorage locals) {
        switch(instruction.getName()) {
            case "vreturn" -> state.returnFunction(null, 0);
            case "areturn", "lreturn" -> state.returnFunction(state.stack.pop(8).value(), 8);
            case "push_string" -> state.stack.push(new StringImpl((String) instruction.getData()[0]), 8);
            case "invoke" -> Interpreter.runFunction(file, state, (Function) instruction.getData()[0]);
            case "icastl" -> state.stack.push((long) (Integer) state.stack.pop(4).value(), 8);
            case "ipush" -> state.stack.push((Integer) instruction.getData()[0], 4);
            case "lpush" -> state.stack.push((Long) instruction.getData()[0], 8);
            case "ladd" -> state.stack.push((Long) state.stack.pop(8).value() + (Long) state.stack.pop(8).value(), 8);
            case "land" -> state.stack.push((Long) state.stack.pop(8).value() & (Long) state.stack.pop(8).value(), 8);
            case "lor" -> state.stack.push((Long) state.stack.pop(8).value() | (Long) state.stack.pop(8).value(), 8);
            case "lleu" -> {
                var a = (Long) state.stack.pop(8).value();
                var b = (Long) state.stack.pop(8).value();
                state.stack.push(Long.compareUnsigned(a, b) <= 0 ? 1L : 0L, 8);
            }
            case "lgeu" -> {
                var a = (Long) state.stack.pop(8).value();
                var b = (Long) state.stack.pop(8).value();
                state.stack.push(Long.compareUnsigned(a, b) >= 0 ? 1L : 0L, 8);
            }
            case "neqjmp" -> {
                if((Long) state.stack.pop(8).value() == 0) {
                    state.currentInstruction += (Short) instruction.getData()[0] - 1;
                }
            }
            case "apop", "lpop" -> state.stack.pop(8);
            case "lload_field" -> state.stack.push(state.getFieldValue((Field) instruction.getData()[0]), 8);
            case "lload_local", "aload_local" -> state.stack.push(locals.get((Short) instruction.getData()[0], 8).value(), 8);
            case "astore_local", "lstore_local" -> locals.set(state.stack.pop(8).value(), (Short) instruction.getData()[0], 8);
            case "class_field_load" -> {
                var val = state.stack.pop(8).value();
                var clz = (Class) instruction.getData()[0];
                var fname = (String) instruction.getData()[1];
                if(val instanceof ClassImpl ci) {
                    ClassField fi = null;
                    for(var c : clz.fields()) {
                        if(c.name().equals(fname)) {
                            fi = c;
                            break;
                        }
                    }
                    if(fi == null) {
                        throw new RuntimeException("Unknown field '" + fname + "' in " + clz.namespace() + "." + clz.name());
                    }
                    var value = ci.getFieldValue(file, state, fname);
                    if(value instanceof Byte) {
                        if(!(fi.type() instanceof PrimitiveType t && t.size == 1)) {
                            throw new IllegalArgumentException("Field type and return value don't match");
                        }
                        state.stack.push(value, 1);
                    }
                    else if(value instanceof Short) {
                        if(!(fi.type() instanceof PrimitiveType t && t.size == 2)) {
                            throw new IllegalArgumentException("Field type and return value don't match");
                        }
                        state.stack.push(value, 2);
                    }
                    else if(value instanceof Integer) {
                        if(!(fi.type() instanceof PrimitiveType t && t.size == 4)) {
                            throw new IllegalArgumentException("Field type and return value don't match");
                        }
                        state.stack.push(value, 4);
                    }
                    else state.stack.push(value, 8);
                }else {
                    throw new IllegalStateException("Expected a class type, got " + val.getClass().getSimpleName() + " (" + val + ")");
                }
            }
            default -> throw new NotImplementedException("Cannot handle instruction {" + instruction + "}");
        }
    }

}
