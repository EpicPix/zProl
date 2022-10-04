package ga.epicpix.zprol.types;

import java.util.Objects;

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

    public String normalName() {
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

    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        PrimitiveType that = (PrimitiveType) o;
        return size == that.size && unsigned == that.unsigned && Objects.equals(descriptor, that.descriptor) && Objects.equals(name, that.name);
    }

    public int hashCode() {
        return Objects.hash(size, unsigned, descriptor, name);
    }
}
