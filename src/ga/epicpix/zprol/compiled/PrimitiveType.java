package ga.epicpix.zprol.compiled;

public class PrimitiveType extends Type {

    public final int size;
    public final boolean unsigned;
    public final String descriptor;
    public final String name;

    public PrimitiveType(int size, boolean unsigned, String descriptor, String name) {
        this.size = size;
        this.unsigned = unsigned;
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
        return "PrimitiveType(" + descriptor + " [" + name + "])";
    }

    public boolean isUnsigned() {
        return unsigned;
    }

    public int getSize() {
        return size;
    }

}
