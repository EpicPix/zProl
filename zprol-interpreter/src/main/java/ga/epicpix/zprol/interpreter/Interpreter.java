package ga.epicpix.zprol.interpreter;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zprol.structures.*;
import ga.epicpix.zprol.types.PrimitiveType;
import ga.epicpix.zprol.types.Type;
import ga.epicpix.zprol.types.Types;

import java.util.List;

import static ga.epicpix.zprol.interpreter.InstructionImpl.runInstruction;

public class Interpreter {

    public static VMState runInterpreter(GeneratedData file, Function function) {
        return runInterpreter(file, function, new DefaultNativeImpl(), new DefaultMemoryImpl());
    }

    public static VMState runInterpreter(GeneratedData file, Function function, NativeImpl natives, MemoryImpl memory) {
        if(memory == null) throw new NullPointerException("The memory implementation cannot be null!");
        VMState state = new VMState(natives, memory);
        for(Field f : file.fields) {
            FieldStorage v = new FieldStorage();
            v.field = f;
            if(f.defaultValue != null) {
                v.value = f.defaultValue.value;
                v.defined = true;
            }
            state.fields.add(v);
        }
        FunctionSignature sig = new FunctionSignature(Types.getTypeFromDescriptor("V"));
        try {
            for(Function f : file.functions) {
                if(f.name.equals(".init") && f.signature.equals(sig)) {
                    runFunction(file, state, f);
                }
            }
            if(function != null) {
                runFunction(file, state, function);
            }
            return state;
        }catch(RuntimeException e) {
            StringBuilder s = new StringBuilder("An exception occurred while interpreting code\n-----------------\nStack Dump:");
            for(DataValue t : state.stack.valueStack()) {
                s.append("\n").append("[").append(t.size).append("b] ").append(t.value);
            }
            s.append("\nNative Implementation: ").append(state.natives == null ? "<None>" : state.natives.getClass().getName());
            s.append("\nMemory Implementation: ").append(state.memory.getClass().getName());
            if(state.memory instanceof DefaultMemoryImpl) {
                DefaultMemoryImpl dmi = (DefaultMemoryImpl) state.memory;
                s.append("\nMemory Maps:");
                for(MemoryData map : dmi.maps) {
                    s.append("\n\t0x").append(Long.toHexString(map.start)).append(" - 0x").append(Long.toHexString(map.start + map.length)).append(" (").append(map.length).append(" bytes)");
                }
                s.append("\nPointer Memory Maps:");
                for(ObjectMemoryData map : dmi.pointerMaps) {
                    s.append("\n\t0x").append(Long.toHexString(map.start)).append(" - 0x").append(Long.toHexString(map.start + (map.count << 3))).append(" (").append(map.count).append(" elements)");
                }
            }
            s.append("\n-----------------");
            throw new RuntimeException(s.toString(), e);
        }
    }

    public static void runFunction(GeneratedData file, VMState state, Function function) {
        LocalStorage locals = new LocalStorage();
        Type[] params = function.signature.parameters;
        int loc = 0;
        for(Type param : params) {
            if(param instanceof PrimitiveType) {
                PrimitiveType prim = (PrimitiveType) param;
                loc += prim.size;
            }
            else loc += 8;
        }

        for(Type param : params) {
            int size;
            if(param instanceof PrimitiveType) {
                PrimitiveType prim = (PrimitiveType) param;
                size = prim.size;
            }
            else size = 8;
            locals.set(state.stack.pop(size).value, loc, size);
            loc -= size;
        }
        state.pushFunction(function);
        Function func = state.currentFunction();
        if(func.modifiers.contains(FunctionModifiers.NATIVE)) {
            if(state.natives == null) {
                throw new IllegalStateException("Cannot call native functions, missing a native implementation");
            }
            Object returnValue = state.natives.runNativeFunction(file, state, locals);
            state.popFunction();
            if(func.signature.returnType != Types.getTypeFromDescriptor("V")) {
                if(func.signature.returnType instanceof PrimitiveType) {
                    PrimitiveType prim = (PrimitiveType) func.signature.returnType;
                    state.stack.push(returnValue, prim.size);
                }
                else state.stack.push(returnValue, 8);
            }
        }else {
            int x = state.currentInstruction;
            boolean returned = false;
            state.currentInstruction = 0;
            List<IBytecodeInstruction> instructions = func.code.getInstructions();
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

    public static void runMethod(GeneratedData file, VMState state, Method method) {
        LocalStorage locals = new LocalStorage();
        Type[] params = method.signature.parameters;
        int loc = 8;
        for(Type param : params) {
            if(param instanceof PrimitiveType) {
                PrimitiveType prim = (PrimitiveType) param;
                loc += prim.size;
            }
            else loc += 8;
        }

        locals.set(state.stack.pop(8).value, loc, 8);
        loc -= 8;
        for(Type param : params) {
            int size;
            if(param instanceof PrimitiveType) {
                PrimitiveType prim = (PrimitiveType) param;
                size = prim.size;
            }
            else size = 8;
            locals.set(state.stack.pop(size).value, loc, size);
            loc -= size;
        }
        state.pushMethod(method);
        Method m = state.currentMethod();
        if(m.modifiers.contains(FunctionModifiers.NATIVE)) {
            if(state.natives == null) {
                throw new IllegalStateException("Cannot call native methods, missing a native implementation");
            }
            Object returnValue = state.natives.runNativeMethod(file, state, locals);
            state.popMethod();
            if(m.signature.returnType != Types.getTypeFromDescriptor("V")) {
                if(m.signature.returnType instanceof PrimitiveType) {
                    PrimitiveType prim = (PrimitiveType) m.signature.returnType;
                    state.stack.push(returnValue, prim.size);
                }
                else state.stack.push(returnValue, 8);
            }
        }else {
            int x = state.currentInstruction;
            boolean returned = false;
            state.currentInstruction = 0;
            List<IBytecodeInstruction> instructions = m.code.getInstructions();
            while(state.currentInstruction < instructions.size()) {
                runInstruction(file, state, instructions.get(state.currentInstruction++), locals);
                if(state.hasReturned) {
                    returned = true;
                    state.popMethod();
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
