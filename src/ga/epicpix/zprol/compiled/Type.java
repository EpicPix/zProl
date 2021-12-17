package ga.epicpix.zprol.compiled;

public class Type {

    public final char id;
    public final String name;

    public Type(char type, String name) {
        id = type;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "Type(" + (int) id + " [" + name + "])";
    }

    public boolean isNumberType() {
        return true;
    }

    public int getSize() {
        return (int) Math.pow(2,  id & 0b0000000000000111) / 2;
    }

    public boolean isUnsigned() {
        return ((id & 0b0000000000001000) >> 3) == 1;
    }

    public boolean isPointer() {
        return ((id & 0b0000000000010000) << 4) == 1;
    }
}
