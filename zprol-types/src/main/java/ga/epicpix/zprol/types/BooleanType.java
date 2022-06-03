package ga.epicpix.zprol.types;

public class BooleanType extends Type {

    public String getDescriptor() {
        return "b";
    }

    public String getName() {
        return "boolean";
    }

    public String normalName() {
        return "boolean";
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }
}
