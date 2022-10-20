package ga.epicpix.zprol.interpreter;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.structures.Class;
import ga.epicpix.zprol.structures.Field;
import ga.epicpix.zprol.structures.Function;
import ga.epicpix.zprol.structures.IBytecodeInstruction;

import java.util.Objects;

class InstructionImpl {

    static void runInstruction(GeneratedData file, VMState state, IBytecodeInstruction instruction, LocalStorage locals) {
        switch(instruction.getName()) {
            case "vreturn" -> state.returnFunction(null, 0);
            case "push_string" -> state.stack.push(instruction.getData()[0], 8);
            case "invoke" -> Interpreter.runFunction(file, state, (Function) instruction.getData()[0]);
            case "lpush" -> state.stack.push((Long) instruction.getData()[0], 8);
            case "lpop" -> state.stack.pop(8);
            case "lload_field" -> state.stack.push(state.getFieldValue((Field) instruction.getData()[0]), 8);
            case "lload_local", "aload_local" -> state.stack.push(locals.get((Short) instruction.getData()[0], 8).value(), 8);
            case "lstore_local" -> locals.set(state.stack.pop(8), (Short) instruction.getData()[0], 8);
            case "class_field_load" -> {
                var val = state.stack.pop(8).value();
                var clz = (Class) instruction.getData()[0];
                var fname = (String) instruction.getData()[1];
                if(val instanceof String str && Objects.equals(clz.namespace(), "zprol.lang") && clz.name().equals("String")) {
                    if(fname.equals("bytes")) {
                        state.stack.push(str.getBytes(), 8);
                        return;
                    }else if(fname.equals("length")) {
                        state.stack.push((long) str.length(), 8);
                        return;
                    }
                }
                throw new NotImplementedException("Cannot handle instruction {" + instruction + "}");
            }
            default -> throw new NotImplementedException("Cannot handle instruction {" + instruction + "}");
        }
    }

}
