package ga.epicpix.zprol.compiled.bytecode;

public enum BytecodeInstructions {

    ADD8(0x00, 0),  // Add 8 bits
    ADD16(0x01, 0), // Add 16 bits
    ADD32(0x02, 0), // Add 32 bits
    ADD64(0x03, 0), // Add 64 bits

    SUB8(0x04, 0),  // Subtract 8 bits
    SUB16(0x05, 0), // Subtract 16 bits
    SUB32(0x06, 0), // Subtract 32 bits
    SUB64(0x07, 0), // Subtract 64 bits

    MULS8(0x08, 0),  // Multiply signed 8 bits
    MULU8(0x09, 0),  // Multiply unsigned 8 bits
    MULS16(0x0A, 0), // Multiply signed 16 bits
    MULU16(0x0B, 0), // Multiply unsigned 16 bits
    MULS32(0x0C, 0), // Multiply signed 32 bits
    MULU32(0x0D, 0), // Multiply unsigned 32 bits
    MULS64(0x0E, 0), // Multiply signed 64 bits
    MULU64(0x0F, 0), // Multiply unsigned 64 bits

    DIVS8(0x10, 0),  // Divide signed 8 bits
    DIVU8(0x11, 0),  // Divide unsigned 8 bits
    DIVS16(0x12, 0), // Divide signed 16 bits
    DIVU16(0x13, 0), // Divide unsigned 16 bits
    DIVS32(0x14, 0), // Divide signed 32 bits
    DIVU32(0x15, 0), // Divide unsigned 32 bits
    DIVS64(0x16, 0), // Divide signed 64 bits
    DIVU64(0x17, 0), // Divide unsigned 64 bits

    PUSHI8(0x18, 1), // Push 8 bit immediate
    PUSHI16(0x19, 2), // Push 16 bit immediate
    PUSHI32(0x1A, 4), // Push 32 bit immediate
    PUSHI64(0x1B, 8), // Push 64 bit immediate

    POP32(0x1C, 0), // Pop 32 bit
    POP64(0x1D, 0), // Pop 64 bit

    INVOKESTATIC(0x1E, 2), // Invoke a static function
    INVOKEDYNAMIC(0x1F, 2), // Invoke a dynamic function

    GETSTATICFIELD(0x20, 2), // Get value from a static field
    SETSTATICFIELD(0x21, 2), // Set value of a static field

    GETFIELD(0x22, 4), // Get value from an object/struct field
    SETFIELD(0x23, 4), // Set value of an object/struct field

    STORE32(0x24, 2), // Store popped 32 bit value in local variable
    STORE64(0x25, 2), // Store popped 64 bit value in local variable and local variable + 1

    LOAD32(0x26, 2), // Load 32 bit value from local variable
    LOAD64(0x27, 2), // Load 64 bit value from local variable and local variable + 1

    AND8(0x28, 0),  // And 8 bits
    AND16(0x29, 0), // And 16 bits
    AND32(0x2A, 0), // And 32 bits
    AND64(0x2B, 0), // And 64 bits

    ;

    private final int id;
    private final int operandSize;

    BytecodeInstructions(int id, int operandSize) {
        this.id = id;
        this.operandSize = operandSize;
    }

    public int getId() {
        return id;
    }

    public int getOperandSize() {
        return operandSize;
    }
}
