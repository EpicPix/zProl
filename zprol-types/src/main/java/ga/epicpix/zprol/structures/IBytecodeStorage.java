package ga.epicpix.zprol.structures;

import java.util.List;

public interface IBytecodeStorage {

    public void pushInstruction(IBytecodeInstruction instruction);
    public void pushInstruction(int index, IBytecodeInstruction instruction);
    public List<IBytecodeInstruction> getInstructions();

    public void setLocalsSize(int size);
    public int getLocalsSize();

}
