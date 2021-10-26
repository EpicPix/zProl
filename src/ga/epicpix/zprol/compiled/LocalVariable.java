package ga.epicpix.zprol.compiled;

public class LocalVariable {

    public String name;
    public Type type;
    public int index;

    public LocalVariable() {}

    public LocalVariable(String name, Type type, int index) {
        this.name = name;
        this.type = type;
        this.index = index;
    }
}
