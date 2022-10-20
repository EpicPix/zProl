package ga.epicpix.zprol.interpreter;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zprol.structures.Function;
import ga.epicpix.zprol.types.PrimitiveType;
import ga.epicpix.zprol.types.Type;

import static ga.epicpix.zprol.interpreter.InstructionImpl.runInstruction;

public class Interpreter {

    public static void runInterpreter(GeneratedData file, Function function) {
        VMState state = new VMState();
        runFunction(file, state, function);
    }

    static void runFunction(GeneratedData file, VMState state, Function function) {
        LocalStorage locals = new LocalStorage();
        var params = function.signature().parameters();
        int loc = 0;
        for(Type param : params) {
            int size = param instanceof PrimitiveType prim ? prim.size : 8;
            locals.set(state.stack.pop(size).value(), loc, size);
        }
        state.pushFunction(function);
        for(var instr : state.currentFunction().code().getInstructions()) {
            runInstruction(file, state, instr, locals);
        }
        state.popFunction();
    }

}
