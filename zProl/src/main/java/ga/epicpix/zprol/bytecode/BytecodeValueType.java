package ga.epicpix.zprol.bytecode;

public enum BytecodeValueType {

    BYTE(1),
    SHORT(2),
    INT(4),
    LONG(8),
    STRING(4),
    FUNCTION(4),
    CLASS(4);

    private final int size;

    private BytecodeValueType(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }
}
