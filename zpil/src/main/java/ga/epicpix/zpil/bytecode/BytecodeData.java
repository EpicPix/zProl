package ga.epicpix.zpil.bytecode;

import ga.epicpix.zprol.structures.IBytecodeInstruction;
import ga.epicpix.zprol.structures.IBytecodeStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class BytecodeData implements IBytecodeStorage {

    private final ArrayList<IBytecodeInstruction> instructions = new ArrayList<>();
    private int localsSize;

    public void removeInstruction(int index) {
        instructions.remove(index);
    }

    public void pushInstruction(IBytecodeInstruction instruction) {
        instructions.add(instruction);
    }
    public void pushInstruction(int index, IBytecodeInstruction instruction) {
        instructions.add(index, instruction);
    }

    public List<IBytecodeInstruction> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }

    public int getInstructionsLength() {
        return instructions.size();
    }

    public void setLocalsSize(int size) {
        localsSize = size;
    }

    public int getLocalsSize() {
        return localsSize;
    }
}
