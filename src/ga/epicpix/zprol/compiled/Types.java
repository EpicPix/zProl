package ga.epicpix.zprol.compiled;

public enum Types {

    INT8(0, 1), INT16(1, 2), INT32(2, 4), INT64(3, 8),
    UINT8(4, 1), UINT16(5, 2), UINT32(6, 4), UINT64(7, 8),
    NONE(14, 0), VOID(15, 0),
    POINTER(8, 8), NAMED(16, 0, true),
    FUTURE_OBJECT(12, 8), OBJECT(9, 8, true),
    FUTURE_STRUCTURE(13, 8), STRUCTURE(10, 8, true),
    FUNCTION_SIGNATURE(11, 8, true),

    ;

    public final int id;
    public final int memorySize;
    public final boolean additionalData;

    Types(int id, int memorySize, boolean additionalData) {
        this.id = id;
        this.memorySize = memorySize;
        this.additionalData = additionalData;
    }

    Types(int id, int memorySize) {
        this(id, memorySize, false);
    }

    public boolean isNumberType() {
        return this == INT8  || this == INT16  || this == INT32  || this == INT64  ||
                this == UINT8 || this == UINT16 || this == UINT32 || this == UINT64 ||
                this == POINTER;
    }

    public boolean isUnsignedNumber() {
        return this == UINT8 || this == UINT16 || this == UINT32 || this == UINT64 ||
               this == POINTER;
    }

}
