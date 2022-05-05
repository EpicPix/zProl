package ga.epicpix.zprol.bytecode;

import java.util.List;

public interface IBytecodeStorage {

    public void pushInstruction(IBytecodeInstruction instruction);
    public List<IBytecodeInstruction> getInstructions();

    public void setLocalsSize(int size);
    public int getLocalsSize();

}
