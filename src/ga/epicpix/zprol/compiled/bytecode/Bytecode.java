package ga.epicpix.zprol.compiled.bytecode;

import ga.epicpix.zprol.compiled.LocalScope;
import ga.epicpix.zprol.compiled.LocalVariable;
import ga.epicpix.zprol.compiled.Type;
import ga.epicpix.zprol.exceptions.VariableNotDefinedException;
import ga.epicpix.zprol.exceptions.VariableAlreadyDefinedException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Bytecode {

    private ArrayList<BytecodeInstruction> instructions = new ArrayList<>();
    private LocalScope currentScope = new LocalScope();
    private ArrayList<String> strings = new ArrayList<>();

    public short addString(String str) {
        for(int i = 0; i<strings.size(); i++) {
            if(strings.get(i).equals(str)) {
                return (short) i;
            }
        }
        strings.add(str);
        return (short) (strings.size() - 1);
    }

    public ArrayList<String> getStrings() {
        return new ArrayList<>(strings);
    }

    public ArrayList<BytecodeInstruction> getInstructions() {
        return new ArrayList<>(instructions);
    }

    public BytecodeInstruction pushInstruction(BytecodeInstructions instruction, Object... data) {
        BytecodeInstruction in = new BytecodeInstruction(instruction, data);
        instructions.add(in);
        return in;
    }

    public BytecodeInstruction pushSizedInstruction(String instruction, int size, Object... data) {
        BytecodeInstruction in = new BytecodeInstruction(BytecodeInstruction.instructionMapping.get(instruction + (size * 8)), data);
        instructions.add(in);
        return in;
    }

    public int getLocalVariablesSize() {
        return currentScope.getLocalVariablesSize();
    }

    public LocalScope newScope() {
        return currentScope = new LocalScope(currentScope);
    }

    public LocalScope leaveScope() {
        if(currentScope.parent == null) {
            throw new IllegalArgumentException("Cannot leave scope, no scopes available!");
        }
        currentScope.parent.addMin(currentScope.getScopeLocalVariableScope());
        return currentScope = currentScope.parent;
    }

    public LocalScope getCurrentScope() {
        return currentScope;
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeShort(getCurrentScope().getLocalVariablesSize());
        out.writeInt(instructions.size());
        for(BytecodeInstruction instr : instructions) {
            instr.write(out);
        }
        out.writeShort(strings.size());
        for(String str : strings) {
            out.writeUTF(str);
        }
    }

    public void load(DataInputStream in) throws IOException {
        getCurrentScope().addMin(in.readUnsignedShort());
        int codeLength = in.readInt();
        for(int i = 0; i<codeLength; i++) {
            BytecodeInstructions instr = BytecodeInstructions.fromOpcode(in.readUnsignedByte());
            int opSize = instr.getOperandSize();
            if(opSize == -1) {
                throw new RuntimeException("Unhandled instruction with variable size: " + instr);
            }else if(opSize == 0) {
                instructions.add(new BytecodeInstruction(instr));
            }else if(opSize == 1) {
                instructions.add(new BytecodeInstruction(instr, in.readByte()));
            }else if(opSize == 2) {
                instructions.add(new BytecodeInstruction(instr, in.readShort()));
            }else if(opSize == 4) {
                instructions.add(new BytecodeInstruction(instr, in.readInt()));
            }else if(opSize == 8) {
                instructions.add(new BytecodeInstruction(instr, in.readLong()));
            }else {
                throw new RuntimeException("Unknown instruction size: " + opSize + " in instruction " + instr);
            }
        }
        int stringsLength = in.readUnsignedShort();
        for(int i = 0; i<stringsLength; i++) {
            addString(in.readUTF());
        }
    }

}
