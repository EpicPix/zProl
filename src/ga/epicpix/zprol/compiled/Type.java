package ga.epicpix.zprol.compiled;

public class Type {

    public final Types type;

    public Type(Types type) {
        this.type = type;
    }

    public String toString() {
        return type.name().toLowerCase();
    }
}
