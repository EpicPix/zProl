package ga.epicpix.zprol.compiled.bytecode;

import java.io.DataOutputStream;
import java.io.IOException;

public class BytecodeInstruction {

    private final BytecodeInstructions instruction;
    private final Object[] data;

    public BytecodeInstruction(BytecodeInstructions instruction, Object[] data) {
        this.instruction = instruction;
        this.data = data;
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeByte(instruction.getId());
        int wrote = 0;
        for(Object obj : data) {
            if(obj instanceof Byte) {
                out.writeByte((byte) obj);
                wrote+=1;
            }else if(obj instanceof Short) {
                out.writeShort((short) obj);
                wrote+=2;
            }else if(obj instanceof Integer) {
                out.writeInt((int) obj);
                wrote+=4;
            }else if(obj instanceof Long) {
                out.writeLong((long) obj);
                wrote+=8;
            }
        }
        if(instruction.getOperandSize() != -1 && instruction.getOperandSize() != wrote) {
            throw new RuntimeException("Invalid amount of bytes wrote while writing the instruction, expected " + instruction.getOperandSize() + " got " + wrote + " (" + instruction + ")");
        }
    }

}
