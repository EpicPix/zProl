package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.bytecode.IBytecodeStorage;

import java.util.EnumSet;

public record Function(String namespace, EnumSet<FunctionModifiers> modifiers, String name, FunctionSignature signature, IBytecodeStorage code) implements IConstantPoolPreparable {

    public void prepareConstantPool(ConstantPool pool) {
        if(!FunctionModifiers.isEmptyCode(modifiers)) {
            for (var instruction : code.getInstructions()) {
                instruction.prepareConstantPool(pool);
            }
        }
    }

    public String toString() {
        return "Function[\"" + (namespace != null ? namespace + "." : "") + name + "\" \"" + signature + "\"" + (modifiers.size() != 0 ? " " + modifiers : "") + "]";
    }
}
