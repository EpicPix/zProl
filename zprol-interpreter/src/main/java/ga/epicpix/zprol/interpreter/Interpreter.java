package ga.epicpix.zprol.interpreter;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zprol.structures.Function;
import ga.epicpix.zprol.structures.FunctionModifiers;
import ga.epicpix.zprol.structures.FunctionSignature;
import ga.epicpix.zprol.types.PrimitiveType;
import ga.epicpix.zprol.types.Types;

import static ga.epicpix.zprol.interpreter.InstructionImpl.runInstruction;

public class Interpreter {

    public static VMState runInterpreter(GeneratedData file, Function function) {
        return runInterpreter(file, function, new DefaultNativeImpl(), new DefaultMemoryImpl());
    }

    public static VMState runInterpreter(GeneratedData file, Function function, NativeImpl natives, MemoryImpl memory) {
        if(memory == null) throw new NullPointerException("The memory implementation cannot be null!");
        VMState state = new VMState(natives, memory);
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
        try {
            for(var f : file.functions) {
                if(f.name().equals(".init") && f.signature().equals(sig)) {
                    runFunction(file, state, f);
                }
            }
            if(function != null) {
                runFunction(file, state, function);
            }
            return state;
        }catch(RuntimeException e) {
            StringBuilder s = new StringBuilder("An exception occurred while interpreting code\nStack Dump:");
            for(var t : state.stack.valueStack()) {
                s.append("\n").append("[").append(t.size()).append("b] ").append(t.value());
            }
            s.append("\n").append("Native Implementation: ").append(state.natives == null ? "<None>" : state.natives.getClass().getName());
            s.append("\n").append("Memory Implementation: ").append(state.memory.getClass().getName());
            throw new RuntimeException(s.toString(), e);
        }
    }

    public static void runFunction(GeneratedData file, VMState state, Function function) {
        LocalStorage locals = new LocalStorage();
        var params = function.signature().parameters();
        int loc = 0;
        for(var param : params) loc += param instanceof PrimitiveType prim ? prim.size : 8;

        for(var param : params) {
            int size = param instanceof PrimitiveType prim ? prim.size : 8;
            locals.set(state.stack.pop(size).value(), loc, size);
            loc -= size;
        }
        state.pushFunction(function);
        var func = state.currentFunction();
        if(func.modifiers().contains(FunctionModifiers.NATIVE)) {
            if(state.natives == null) {
                throw new IllegalStateException("Cannot call native functions, missing a native implementation");
            }
            Object returnValue = state.natives.runNative(file, state, locals);
            state.popFunction();
            if(func.signature().returnType() != Types.getTypeFromDescriptor("V")) {
                state.stack.push(returnValue, func.signature().returnType() instanceof PrimitiveType prim ? prim.size : 8);
            }
        }else {
            int x = state.currentInstruction;
            boolean returned = false;
            state.currentInstruction = 0;
            var instructions = func.code().getInstructions();
            while(state.currentInstruction < instructions.size()) {
                runInstruction(file, state, instructions.get(state.currentInstruction++), locals);
                if(state.hasReturned) {
                    returned = true;
                    state.popFunction();
                    if(state.returnSize != 0) {
                        state.stack.push(state.returnValue, state.returnSize);
                        state.returnValue = null;
                    }
                    state.hasReturned = false;
                    break;
                }
            }
            if(!returned) {
                throw new IllegalStateException("Function has not returned!");
            }
            state.currentInstruction = x;
        }
    }

}