package ga.epicpix.zprol.compiled;

public class PrimitiveType extends Type {

    public final char id;
    public final String name;

    public PrimitiveType(char type, String name) {
        id = type;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "PrimitiveType(" + (int) id + " [" + name + "])";
    }

    public int getSize() {
        return (int) Math.pow(2, id & 0b0000000000000111) / 2;
    }

}
