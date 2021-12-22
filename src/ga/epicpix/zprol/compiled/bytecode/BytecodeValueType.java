package ga.epicpix.zprol.compiled.bytecode;

public enum BytecodeValueType {

    BYTE(1),
    SHORT(2),
    INT(4),
    LONG(8),
    CONSTANT_POOL_INDEX(2);

    private final int size;

    private BytecodeValueType(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }
}
