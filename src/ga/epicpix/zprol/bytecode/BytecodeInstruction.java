package ga.epicpix.zprol.bytecode;

import ga.epicpix.zprol.compiled.Class;
import ga.epicpix.zprol.compiled.generated.ConstantPool;
import ga.epicpix.zprol.compiled.Function;
import ga.epicpix.zprol.bytecode.Bytecode.BytecodeInstructionData;

import java.util.Arrays;

record BytecodeInstruction(BytecodeInstructionData data, Object[] args) implements IBytecodeInstruction {

    public String toString() {
        return getName() + (args.length != 0 ? " " + Arrays.toString(args).replace("\n", "\\n").replace("\0", "\\0") : "");
    }

    public int getId() {
        return data.id();
    }

    public String getName() {
        return data.name();
    }

    public Object[] getData() {
        return args;
    }

    public void prepareConstantPool(ConstantPool pool) {
        var values = data.values();
        for(int i = 0; i<values.length; i++) {
            var type = values[i];
            var val = args[i];
            if(type == BytecodeValueType.STRING) {
                if(val instanceof String v) pool.getOrCreateStringIndex(v);
                else throw new IllegalArgumentException(val.toString().getClass().getName());
            }else if(type == BytecodeValueType.FUNCTION) {
                if(val instanceof Function v) pool.getOrCreateFunctionIndex(v);
                else throw new IllegalArgumentException(val.toString().getClass().getName());
            }else if(type == BytecodeValueType.CLASS) {
                if(val instanceof Class v) pool.getOrCreateClassIndex(v);
                else throw new IllegalArgumentException(val.toString().getClass().getName());
            }
        }
    }
}
