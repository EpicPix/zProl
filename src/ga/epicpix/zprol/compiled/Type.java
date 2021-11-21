package ga.epicpix.zprol.compiled;

public class Type {

    public Types type;
    public final char id;

    @Deprecated
    public Type(Types type) {
        this.type = type;
        id = 0xffff;
    }

    public Type(char type) {
        id = type;
    }

    public String toString() {
        return "Type(" + (int) id + ")";
    }

    public boolean isNumberType() {
        return type.isNumberType();
    }

    public boolean isUnsigned() {
        return ((id & 0b0000000000000100) >> 2) == 1;
    }

    public int getSize() {
        return id & 0b0000000000000011;
    }

}
