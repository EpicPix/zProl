package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.compiled.bytecode.BytecodeInstruction;

public class CompileOperation {

    public final BytecodeInstruction instruction;
    public final CompileOperationType type;

    public CompileOperation(BytecodeInstruction instruction, CompileOperationType type) {
        this.instruction = instruction;
        this.type = type;
    }

}
