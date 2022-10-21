package ga.epicpix.zprol.data;

import java.util.Objects;

public final class ConstantValue {
    public final Object value;

    public ConstantValue(Object value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        if(obj == null || obj.getClass() != this.getClass()) return false;
        ConstantValue that = (ConstantValue) obj;
        return Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "ConstantValue[" +
            "value=" + value + ']';
    }
}
