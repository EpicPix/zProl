package ga.epicpix.zprol.structures;

import java.util.EnumSet;

public record Function(String namespace, EnumSet<FunctionModifiers> modifiers, String name, FunctionSignature signature, IBytecodeStorage code) {

    public String toString() {
        return "Function[\"" + (namespace != null ? namespace + "." : "") + name + "\" \"" + signature + "\"" + (modifiers.size() != 0 ? " " + modifiers : "") + "]";
    }
}
