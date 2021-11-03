package ga.epicpix.zprol.compiled.bytecode;

import ga.epicpix.zprol.compiled.CompiledData;
import ga.epicpix.zprol.compiled.TypeFunctionSignature;
import java.io.DataOutputStream;
import java.io.IOException;

public class BytecodeInstruction {

    public final BytecodeInstructions instruction;
    public Object[] data;

    public BytecodeInstruction(BytecodeInstructions instruction, Object... data) {
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
            }else if(obj instanceof TypeFunctionSignature) {
                if(instruction.getOperandSize() == -1) {
                    CompiledData.writeFunctionSignatureType((TypeFunctionSignature) obj, out);
                }else {
                    throw new RuntimeException("Tried to write function signature without the size being variable sized");
                }
            }else {
                throw new RuntimeException("Tried to parse unknown object");
            }
        }
        if(instruction.getOperandSize() != -1 && instruction.getOperandSize() != wrote) {
            throw new RuntimeException("Invalid amount of bytes wrote while writing the instruction, expected " + instruction.getOperandSize() + " got " + wrote + " (" + instruction + ")");
        }
    }

    public String toString() {
        return instruction.name().toLowerCase();
    }
}
