package ga.epicpix.zprol.compiled;

public class LocalVariable {

    public String name;
    public Type type;
    public int sizeIndex;

    public LocalVariable() {}

    public LocalVariable(String name, Type type, int sizeIndex) {
        this.name = name;
        this.type = type;
        this.sizeIndex = sizeIndex;
    }
}
