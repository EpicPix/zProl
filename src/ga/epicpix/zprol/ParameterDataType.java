package ga.epicpix.zprol;

public class ParameterDataType {

    public final String type;
    public final String name;

    public ParameterDataType(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String toString() {
        return "Parameter(\"" + name + "\": \"" + type + "\")";
    }
}
