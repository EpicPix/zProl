package ga.epicpix.zprol;

public class StructureType {

    public final String type;
    public final String name;

    public StructureType(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String toString() {
        return "StructureType(type=\"" + type + "\", name=\"" + name + "\")";
    }
}
