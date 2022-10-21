package ga.epicpix.zprol.interpreter;

public final class LocalValue {
    public final Object value;
    public final int size;
    public final int index;

    LocalValue(Object value, int size, int index) {
        this.value = value;
        this.size = size;
        this.index = index;
    }

}
