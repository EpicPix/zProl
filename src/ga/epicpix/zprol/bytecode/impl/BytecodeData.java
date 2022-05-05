package ga.epicpix.zprol.bytecode.impl;

import ga.epicpix.zprol.bytecode.IBytecodeInstruction;
import ga.epicpix.zprol.bytecode.IBytecodeStorage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class BytecodeData implements IBytecodeStorage {

    private final ArrayList<IBytecodeInstruction> instructions = new ArrayList<>();
    private int localsSize;

    public void pushInstruction(IBytecodeInstruction instruction) {
        instructions.add(instruction);
    }

    public List<IBytecodeInstruction> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }

    public void setLocalsSize(int size) {
        localsSize = size;
    }

    public int getLocalsSize() {
        return localsSize;
    }
}
