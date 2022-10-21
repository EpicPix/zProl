package ga.epicpix.zprol.structures;

import ga.epicpix.zprol.data.ConstantValue;
import ga.epicpix.zprol.types.Type;

import java.util.EnumSet;
import java.util.Objects;

public final class Field {
    public final String namespace;
    public final EnumSet<FieldModifiers> modifiers;
    public final String name;
    public final Type type;
    public final ConstantValue defaultValue;

    public Field(String namespace, EnumSet<FieldModifiers> modifiers, String name, Type type, ConstantValue defaultValue) {
        this.namespace = namespace;
        this.modifiers = modifiers;
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        if(obj == null || obj.getClass() != this.getClass()) return false;
        Field that = (Field) obj;
        return Objects.equals(this.namespace, that.namespace) &&
            Objects.equals(this.modifiers, that.modifiers) &&
            Objects.equals(this.name, that.name) &&
            Objects.equals(this.type, that.type) &&
            Objects.equals(this.defaultValue, that.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, modifiers, name, type, defaultValue);
    }

    @Override
    public String toString() {
        return "Field[" +
            "namespace=" + namespace + ", " +
            "modifiers=" + modifiers + ", " +
            "name=" + name + ", " +
            "type=" + type + ", " +
            "defaultValue=" + defaultValue + ']';
    }
}
