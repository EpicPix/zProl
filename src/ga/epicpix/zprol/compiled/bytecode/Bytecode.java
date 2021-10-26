package ga.epicpix.zprol.compiled.bytecode;

import ga.epicpix.zprol.compiled.LocalVariable;
import ga.epicpix.zprol.compiled.Type;
import ga.epicpix.zprol.exceptions.VariableNotDefinedException;
import ga.epicpix.zprol.exceptions.VariableAlreadyDefinedException;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Bytecode {

    private ArrayList<BytecodeInstruction> instructions = new ArrayList<>();
    private int localVariableSizeIndex = 0;
    private ArrayList<LocalVariable> localVariables = new ArrayList<>();

    public void pushInstruction(BytecodeInstructions instruction, Object... data) {
        instructions.add(new BytecodeInstruction(instruction, data));
    }

    public LocalVariable defineLocalVariable(String name, Type type) {
        for(LocalVariable localVar : localVariables) {
            if(localVar.name.equals(name)) {
                throw new VariableAlreadyDefinedException(name);
            }
        }
        LocalVariable localVar = new LocalVariable(name, type, localVariableSizeIndex);
        localVariableSizeIndex += type.type.memorySize;
        localVariables.add(localVar);
        return localVar;
    }

    public LocalVariable getLocalVariable(String name) {
        for(LocalVariable lVar : localVariables) {
            if(lVar.name.equals(name)) {
                return lVar;
            }
        }
        throw new VariableNotDefinedException(name);
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeShort(localVariableSizeIndex);
        out.writeInt(instructions.size());
        for(BytecodeInstruction instr : instructions) {
            instr.write(out);
        }
    }
}
