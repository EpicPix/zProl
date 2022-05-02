package ga.epicpix.zprol.compiled.bytecode;

import ga.epicpix.zprol.compiled.ConstantPool;
import ga.epicpix.zprol.compiled.ConstantPoolEntry;
import ga.epicpix.zprol.compiled.GeneratedData;
import ga.epicpix.zprol.compiled.IConstantPoolPreparable;
import ga.epicpix.zprol.compiled.bytecode.impl.Bytecode;
import ga.epicpix.zprol.exceptions.InvalidOperationException;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface IBytecodeInstruction extends IConstantPoolPreparable {

    public int getId();
    public String getName();
    public Object[] getData();
    public byte[] write(ConstantPool pool) throws IOException;

    public default void write(DataOutput out, ConstantPool pool) throws IOException {
        out.write(write(pool));
    }

    public static IBytecodeInstruction read(DataInput input, ConstantPool pool) throws IOException {
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
            if(type == BytecodeValueType.STRING) {
                args[i] = ((ConstantPoolEntry.StringEntry) pool.entries.get(((Number) args[i]).intValue())).getString();
            }else if(type == BytecodeValueType.FUNCTION) {
                args[i] = pool.entries.get(((Number) args[i]).intValue());
            }
        }
        return instructionGenerator.createInstruction(args);
    }

    public static void postRead(IBytecodeInstruction instr, GeneratedData data) {
        BytecodeValueType[] values = Bytecode.BYTECODE.getInstructionValueTypesRequirements(instr.getId());
        for(int i = 0; i<values.length; i++) {
            if(values[i] == BytecodeValueType.FUNCTION) {
                var entry = (ConstantPoolEntry.FunctionEntry) instr.getData()[i];
                var namespace = ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(entry.getNamespace() - 1)).getString();
                var name = ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(entry.getName() - 1)).getString();
                var signature = ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(entry.getSignature() - 1)).getString();
                instr.getData()[i] = data.getFunction(namespace, name, signature);
            }
        }
    }

}
