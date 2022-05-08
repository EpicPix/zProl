package ga.epicpix.zprol.compiled;

import java.util.Objects;

public class ClassType extends Type {

    private final String namespace;
    private final String name;

    public ClassType(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    public String normalName() {
        return (namespace != null ? namespace + "." : "") + name;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String toString() {
        return "ClassType(" + (namespace != null ? namespace + "." : "") + name + ")";
    }

    public String getDescriptor() {
        return "C" + (namespace != null ? namespace + "." : "") + name + ";";
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassType classType = (ClassType) o;
        return Objects.equals(namespace, classType.namespace) && Objects.equals(name, classType.name);
    }

    public int hashCode() {
        return Objects.hash(namespace, name);
    }
}
