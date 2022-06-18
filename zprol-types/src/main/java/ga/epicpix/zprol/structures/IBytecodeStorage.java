package ga.epicpix.zprol.structures;

import java.util.List;

public interface IBytecodeStorage {

    public default void replaceInstruction(int index, IBytecodeInstruction instruction) {
        removeInstruction(index);
        pushInstruction(index, instruction);
    }

    public void removeInstruction(int index);
    public void pushInstruction(IBytecodeInstruction instruction);
    public void pushInstruction(int index, IBytecodeInstruction instruction);
    public List<IBytecodeInstruction> getInstructions();
    public int getInstructionsLength();

    public void setLocalsSize(int size);
    public int getLocalsSize();

}
