package ga.epicpix.zprol.interpreter;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.structures.Function;
import ga.epicpix.zprol.structures.IBytecodeInstruction;

class InstructionImpl {

    static void runInstruction(GeneratedData file, VMState state, IBytecodeInstruction instruction, LocalStorage locals) {
        switch(instruction.getName()) {
            case "push_string" -> state.stack.push(instruction.getData()[0], 8);
            case "invoke" -> Interpreter.runFunction(file, state, (Function) instruction.getData()[0]);
            default -> throw new NotImplementedException("Cannot handle instruction {" + instruction + "}");
        }
    }

}
