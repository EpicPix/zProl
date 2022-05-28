package ga.epicpix.zprol.types;

import java.util.Objects;

public class ArrayType extends Type {

    public final Type type;

    public ArrayType(Type type) {
        this.type = type;
    }

    public String getDescriptor() {
        return "[" + type.getDescriptor();
    }

    public String getName() {
        return type.getName() + "[]";
    }

    public String normalName() {
        return type.normalName() + "[]";
    }

    public String toString() {
        return getName();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(type, ((ArrayType) o).type);
    }

    public int hashCode() {
        return Objects.hash(type);
    }
}
