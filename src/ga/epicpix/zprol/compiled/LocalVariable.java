package ga.epicpix.zprol.compiled;

public class LocalVariable {

    public String name;
    public PrimitiveType type;
    public int index;

    public LocalVariable(String name, PrimitiveType type, int index) {
        this.name = name;
        this.type = type;
        this.index = index;
    }
}
