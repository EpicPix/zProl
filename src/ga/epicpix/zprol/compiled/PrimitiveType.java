package ga.epicpix.zprol.compiled;

public class PrimitiveType extends Type {

    public final char id;
    public final String descriptor;
    public final String name;

    public PrimitiveType(char type, String descriptor, String name) {
        id = type;
        this.descriptor = descriptor;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String toString() {
        return "PrimitiveType(" + (int) id + " - " + descriptor + " [" + name + "])";
    }

    public boolean isBuiltInType() {
        return (id & 0x8000) == 0x8000;
    }

    public int getSize() {
        if(isBuiltInType()) {
            return (int) Math.pow(2, id & 0x000f) / 2;
        }
        return 8;
    }

}
