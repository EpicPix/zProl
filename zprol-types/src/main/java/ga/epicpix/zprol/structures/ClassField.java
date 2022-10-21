package ga.epicpix.zprol.structures;

import ga.epicpix.zprol.types.Type;

import java.util.Objects;

public final class ClassField {
    public final String name;
    public final Type type;

    public ClassField(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        if(obj == null || obj.getClass() != this.getClass()) return false;
        ClassField that = (ClassField) obj;
        return Objects.equals(this.name, that.name) &&
            Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return "ClassField[" +
            "name=" + name + ", " +
            "type=" + type + ']';
    }
}
