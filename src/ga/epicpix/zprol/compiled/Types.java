package ga.epicpix.zprol.compiled;

public enum Types {

    INT8(0), INT16(1), INT32(2), INT64(3), UINT8(4), UINT16(5), UINT32(6), UINT64(7), NONE(14), VOID(15),
    POINTER(8),
    FUTURE_OBJECT(12), OBJECT(9, true),
    FUTURE_STRUCTURE(13), STRUCTURE(10, true),
    FUNCTION_SIGNATURE(11, true),

    ;

    public final int id;
    public final boolean additionalData;

    Types(int id, boolean additionalData) {
        this.id = id;
        this.additionalData = additionalData;
    }

    Types(int id) {
        this(id, false);
    }

}
