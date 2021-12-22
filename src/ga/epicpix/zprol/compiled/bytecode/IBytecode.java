package ga.epicpix.zprol.compiled.bytecode;

import java.io.IOException;
import java.io.OutputStream;

public interface IBytecode {

    public void registerInstruction(int id, String name, BytecodeValueType... values);

    public IBytecodeInstructionGenerator getInstruction(int id);
    public IBytecodeInstructionGenerator getInstruction(String name);

    public byte[] writeInstructions();

    public default void writeInstructions(OutputStream out) throws IOException {
        out.write(writeInstructions());
    }

}
