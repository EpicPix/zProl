package ga.epicpix.zprol.compiled.bytecode;

import java.io.DataOutput;
import java.io.IOException;

public interface IBytecodeInstruction {

    public int getId();
    public String getName();
    public byte[] write();

    public default void write(DataOutput out) throws IOException {
        out.write(write());
    }

}
