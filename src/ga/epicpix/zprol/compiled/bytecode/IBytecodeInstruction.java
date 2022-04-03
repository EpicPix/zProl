package ga.epicpix.zprol.compiled.bytecode;

import ga.epicpix.zprol.compiled.bytecode.impl.Bytecode;
import ga.epicpix.zprol.exceptions.InvalidOperationException;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface IBytecodeInstruction {

    public int getId();
    public String getName();
    public byte[] write();

    public default void write(DataOutput out) throws IOException {
        out.write(write());
    }

    public static IBytecodeInstruction read(DataInput input) throws IOException {
        int id = input.readUnsignedByte();
        var instructionGenerator = Bytecode.BYTECODE.getInstruction(id);
        BytecodeValueType[] values = Bytecode.BYTECODE.getInstructionValueTypesRequirements(id);
        Object[] args = new Object[values.length];
        for(int i = 0; i<values.length; i++) {
            BytecodeValueType type = values[i];
            args[i] = switch (type.getSize()) {
                case 1 -> input.readByte();
                case 2 -> input.readShort();
                case 4 -> input.readInt();
                case 8 -> input.readLong();
                default -> throw new InvalidOperationException("Invalid size of bytecode type: " + type.getSize());
            };
        }
        return instructionGenerator.createInstruction(args);
    }

}
