package ga.epicpix.zprol.compiled;

public class Type {

    public Types type;
    public final char id;
    public final String name;

    @Deprecated
    public Type(Types type) {
        this.type = type;
        id = 0xffff;
        name = type.name();
    }

    public Type(char type, String name) {
        id = type;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        if(id == 65535) return "Type(" + name + ")";
        return "Type(" + (int) id + ")";
    }

    public boolean isNumberType() {
        return type.isNumberType();
    }

    public int getSize() {
        return (int) Math.pow(id & 0b0000000000000111, 2) / 2;
    }

    public boolean isUnsigned() {
        return ((id & 0b0000000000001000) >> 3) == 1;
    }

    public boolean isPointer() {
        return ((id & 0b0000000000010000) << 4) == 1;
    }
}
