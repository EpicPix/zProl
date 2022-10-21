package ga.epicpix.zprol.structures;

import java.util.EnumSet;

public record Method(String namespace, EnumSet<FunctionModifiers> modifiers, String className, String name, FunctionSignature signature, IBytecodeStorage code) {

    public String toString() {
        return "Method[\"" + (namespace != null ? namespace + "." : "") + className + "." + name + "\" \"" + signature + "\"" + (modifiers.size() != 0 ? " " + modifiers : "") + "]";
    }
}
