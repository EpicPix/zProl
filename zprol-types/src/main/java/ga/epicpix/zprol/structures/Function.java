package ga.epicpix.zprol.structures;

import java.util.EnumSet;
import java.util.Objects;

public final class Function {
    public final String namespace;
    public final EnumSet<FunctionModifiers> modifiers;
    public final String name;
    public final FunctionSignature signature;
    public final IBytecodeStorage code;

    public Function(String namespace, EnumSet<FunctionModifiers> modifiers, String name, FunctionSignature signature, IBytecodeStorage code) {
        this.namespace = namespace;
        this.modifiers = modifiers;
        this.name = name;
        this.signature = signature;
        this.code = code;
    }

    public String toString() {
        return "Function[\"" + (namespace != null ? namespace + "." : "") + name + "\" \"" + signature + "\"" + (modifiers.size() != 0 ? " " + modifiers : "") + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        if(obj == null || obj.getClass() != this.getClass()) return false;
        Function that = (Function) obj;
        return Objects.equals(this.namespace, that.namespace) &&
            Objects.equals(this.modifiers, that.modifiers) &&
            Objects.equals(this.name, that.name) &&
            Objects.equals(this.signature, that.signature) &&
            Objects.equals(this.code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, modifiers, name, signature, code);
    }

}
