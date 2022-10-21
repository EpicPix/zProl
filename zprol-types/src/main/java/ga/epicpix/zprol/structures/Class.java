package ga.epicpix.zprol.structures;

import java.util.Arrays;
import java.util.Objects;

public final class Class {
    public final String namespace;
    public final String name;
    public final ClassField[] fields;
    public final Method[] methods;

    public Class(String namespace, String name, ClassField[] fields, Method[] methods) {
        this.namespace = namespace;
        this.name = name;
        this.fields = fields;
        this.methods = methods;
    }

    public String toString() {
        return "Class[\"" + (namespace != null ? namespace : "") + "\" \"" + name + "\"]";
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        if(obj == null || obj.getClass() != this.getClass()) return false;
        Class that = (Class) obj;
        return Objects.equals(this.namespace, that.namespace) &&
            Objects.equals(this.name, that.name) &&
            Arrays.equals(this.fields, that.fields) &&
            Arrays.equals(this.methods, that.methods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, name, Arrays.hashCode(fields), Arrays.hashCode(methods));
    }

}
