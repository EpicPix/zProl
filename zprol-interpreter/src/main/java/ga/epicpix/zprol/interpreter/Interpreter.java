package ga.epicpix.zprol.interpreter;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zprol.structures.Function;
import ga.epicpix.zprol.structures.FunctionModifiers;
import ga.epicpix.zprol.structures.FunctionSignature;
import ga.epicpix.zprol.types.PrimitiveType;
import ga.epicpix.zprol.types.Types;

import static ga.epicpix.zprol.interpreter.InstructionImpl.runInstruction;

public class Interpreter {

    public static void runInterpreter(GeneratedData file, Function function) {
        runInterpreter(file, function, new DefaultNativeImpl());
    }

    public static void runInterpreter(GeneratedData file, Function function, NativeImpl natives) {
        VMState state = new VMState(natives);
        for(var f : file.fields) {
            var v = new FieldStorage();
            v.field = f;
            if(f.defaultValue() != null) {
                v.value = f.defaultValue().value();
                v.defined = true;
            }
            state.fields.add(v);
        }
        var sig = new FunctionSignature(Types.getTypeFromDescriptor("V"));
        for(var f : file.functions) {
            if(f.name().equals(".init") && f.signature().equals(sig)) {
                runFunction(file, state, f);
            }
        }
        runFunction(file, state, function);
    }

    static void runFunction(GeneratedData file, VMState state, Function function) {
        LocalStorage locals = new LocalStorage();
        var params = function.signature().parameters();
        int loc = 0;
        for(var param : params) loc += param instanceof PrimitiveType prim ? prim.size : 8;

        for(int i = 0; i<params.length; i++) {
            int size = params[i] instanceof PrimitiveType prim ? prim.size : 8;
            locals.set(state.stack.pop(size).value(), loc, size);
            loc -= size;
        }
        state.pushFunction(function);
        var func = state.currentFunction();
        if(func.modifiers().contains(FunctionModifiers.NATIVE)) {
            Object returnValue = state.natives.runNative(file, state, locals);
            state.popFunction();
            if(func.signature().returnType() != Types.getTypeFromDescriptor("V")) {
                state.stack.push(returnValue, func.signature().returnType() instanceof PrimitiveType prim ? prim.size : 8);
            }
        }else {
            boolean returned = false;
            for(var instr : func.code().getInstructions()) {
                runInstruction(file, state, instr, locals);
                if(state.hasReturned) {
                    returned = true;
                    state.popFunction();
                    if(state.returnSize != 0) {
                        state.stack.push(state.returnValue, state.returnSize);
                        state.returnValue = null;
                    }
                    state.hasReturned = false;
                }
            }
            if(!returned) {
                throw new IllegalStateException("Function has not returned!");
            }
        }
    }

}
