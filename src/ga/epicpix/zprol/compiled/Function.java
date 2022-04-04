package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.compiled.bytecode.IBytecodeStorage;

public record Function(String namespace, String name, FunctionSignature signature, IBytecodeStorage code) implements IConstantPoolPreparable {

    public void prepareConstantPool(ConstantPool pool) {
        for(var instruction : code.getInstructions()) {
            instruction.prepareConstantPool(pool);
        }
    }

}
