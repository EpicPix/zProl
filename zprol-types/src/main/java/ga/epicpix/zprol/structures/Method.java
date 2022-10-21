package ga.epicpix.zprol.structures;

import java.util.EnumSet;
import java.util.Objects;

public final class Method {
    public final String namespace;
    public final EnumSet<FunctionModifiers> modifiers;
    public final String className;
    public final String name;
    public final FunctionSignature signature;
    public final IBytecodeStorage code;

    public Method(String namespace, EnumSet<FunctionModifiers> modifiers, String className, String name, FunctionSignature signature, IBytecodeStorage code) {
        this.namespace = namespace;
        this.modifiers = modifiers;
        this.className = className;
        this.name = name;
        this.signature = signature;
        this.code = code;
    }

    public String toString() {
        return "Method[\"" + (namespace != null ? namespace + "." : "") + className + "." + name + "\" \"" + signature + "\"" + (modifiers.size() != 0 ? " " + modifiers : "") + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        if(obj == null || obj.getClass() != this.getClass()) return false;
        Method that = (Method) obj;
        return Objects.equals(this.namespace, that.namespace) &&
            Objects.equals(this.modifiers, that.modifiers) &&
            Objects.equals(this.className, that.className) &&
            Objects.equals(this.name, that.name) &&
            Objects.equals(this.signature, that.signature) &&
            Objects.equals(this.code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, modifiers, className, name, signature, code);
    }

}
