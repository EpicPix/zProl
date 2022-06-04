package ga.epicpix.zprol.types;

public class NullType extends Type {

    public String getDescriptor() {
        throw new RuntimeException("null does not have a descriptor");
    }

    public String getName() {
        return "null";
    }

    public String normalName() {
        return "null";
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }
}
