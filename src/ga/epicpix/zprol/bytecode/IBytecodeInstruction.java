package ga.epicpix.zprol.bytecode;

import ga.epicpix.zprol.compiled.generated.ConstantPool;
import ga.epicpix.zprol.compiled.generated.ConstantPoolEntry;
import ga.epicpix.zprol.compiled.generated.GeneratedData;
import ga.epicpix.zprol.compiled.generated.IConstantPoolPreparable;
import ga.epicpix.zprol.bytecode.impl.Bytecode;

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
                default -> throw new IllegalStateException("Invalid size of bytecode type: " + type.getSize());
            };
            if(type == BytecodeValueType.STRING) {
                args[i] = ((Number) args[i]).intValue();
            }else if(type == BytecodeValueType.FUNCTION) {
                args[i] = ((Number) args[i]).intValue();
            }
        }
        return instructionGenerator.createInstruction(args);
    }

    public static void postRead(IBytecodeInstruction instr, GeneratedData data) {
        BytecodeValueType[] values = Bytecode.BYTECODE.getInstructionValueTypesRequirements(instr.getId());
        for(int i = 0; i<values.length; i++) {
            if(values[i] == BytecodeValueType.STRING) {
                int index = ((Number) instr.getData()[i]).intValue();
                instr.getData()[i] = ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(index - 1)).getString();
            }else if(values[i] == BytecodeValueType.FUNCTION) {
                int index = ((Number) instr.getData()[i]).intValue();
                var entry = (ConstantPoolEntry.FunctionEntry) data.constantPool.entries.get(index - 1);
                var namespace = entry.getNamespace() != 0 ? ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(entry.getNamespace() - 1)).getString() : null;
                var name = ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(entry.getName() - 1)).getString();
                var signature = ((ConstantPoolEntry.StringEntry) data.constantPool.entries.get(entry.getSignature() - 1)).getString();
                instr.getData()[i] = data.getFunction(namespace, name, signature);
            }
        }
    }

}