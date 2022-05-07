package ga.epicpix.zprol.compiled;

public class ClassType extends Type {

    private final String namespace;
    private final String name;

    public ClassType(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
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
}
