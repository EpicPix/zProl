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
    PUSHI64F8(0x1B, 1), // Push 64 bit immediate from 8 bits
    PUSHI64(0x1C, 8), // Push 64 bit immediate

    POP8(0x1D, 0), // Pop 8 bits
    POP16(0x1E, 0), // Pop 16 bits
    POP32(0x1F, 0), // Pop 32 bits
    POP64(0x20, 0), // Pop 64 bits

    INVOKESTATIC(0x21, 2), // Invoke a static function
    INVOKEDYNAMIC(0x22, 2), // Invoke a dynamic function
    INVOKESIGNATURE(0x23, -1), // Invoke a signature function
    PUSHFUNCTION(0x24, 2), // Push string

    GETSTATICFIELD8(0x25, 2), // Get value from a static field that is 8 bits
    GETSTATICFIELD16(0x66, 2), // Get value from a static field that is 16 bits
    GETSTATICFIELD32(0x67, 2), // Get value from a static field that is 32 bits
    GETSTATICFIELD64(0x68, 2), // Get value from a static field that is 64 bits
    SETSTATICFIELD8(0x26, 2), // Set value of a static field that is 8 bits
    SETSTATICFIELD16(0x63, 2), // Set value of a static field that is 16 bits
    SETSTATICFIELD32(0x64, 2), // Set value of a static field that is 32 bits
    SETSTATICFIELD64(0x65, 2), // Set value of a static field that is 64 bits

    GETFIELD(0x27, 4), // Get value from an object/struct field
    SETFIELD(0x28, 4), // Set value of an object/struct field

    STORE8(0x29, 2), // Store popped 8 bit value
    STORE16(0x2A, 2), // Store popped 16 bit value
    STORE32(0x2B, 2), // Store popped 32 bit value
    STORE64(0x2C, 2), // Store popped 64 bit value

    LOAD8(0x2D, 2), // Load 8 bit value and push it on the stack
    LOAD16(0x2E, 2), // Load 16 bit value and push it on the stack
    LOAD32(0x2F, 2), // Load 32 bit value and push it on the stack
    LOAD64(0x30, 2), // Load 64 bit value and push it on the stack

    AND8(0x31, 0),  // And 8 bits
    AND16(0x32, 0), // And 16 bits
    AND32(0x33, 0), // And 32 bits
    AND64(0x34, 0), // And 64 bits

    MODS8(0x35, 0),  // Mod signed 8 bits
    MODU8(0x36, 0),  // Mod unsigned 8 bits
    MODS16(0x37, 0), // Mod signed 16 bits
    MODU16(0x38, 0), // Mod unsigned 16 bits
    MODS32(0x39, 0), // Mod signed 32 bits
    MODU32(0x3A, 0), // Mod unsigned 32 bits
    MODS64(0x3B, 0), // Mod signed 64 bits
    MODU64(0x3C, 0), // Mod unsigned 64 bits

    SHL8(0x3D, 0),  // Shift left popped 8 bits
    SHL16(0x3E, 0), // Shift left popped 16 bits
    SHL32(0x3F, 0), // Shift left popped 32 bits
    SHL64(0x40, 0), // Shift left popped 64 bits

    SHR8(0x41, 0),  // Shift right popped 8 bits
    SHR16(0x42, 0), // Shift right popped 16 bits
    SHR32(0x43, 0), // Shift right popped 32 bits
    SHR64(0x44, 0), // Shift right popped 64 bits

    SYSCALL1(0x45, 0), // Syscall, parameters on stack
    SYSCALL2(0x46, 0), // Syscall, parameters on stack
    SYSCALL3(0x47, 0), // Syscall, parameters on stack
    SYSCALL4(0x48, 0), // Syscall, parameters on stack
    SYSCALL5(0x49, 0), // Syscall, parameters on stack
    SYSCALL6(0x4A, 0), // Syscall, parameters on stack
    SYSCALL7(0x4B, 0), // Syscall, parameters on stack

    PUSHSTR(0x4C, 2), // Push string

    RETURN(0x4D, 0), // Return void
    RETURN8(0x4E, 0), // Return 8 bit
    RETURN16(0x4F, 0), // Return 16 bit
    RETURN32(0x50, 0), // Return 32 bit
    RETURN64(0x51, 0), // Return 64 bit

    COMPARE64(0x52, 0), // Compare 64 bit

    COMPAREN64(0x53, 0), // Compare not 64 bit

    JUMPNE(0x54, 2), // Jump if not equal, depends on COMPARE64
    JUMPE(0x55, 2), // Jump if equal, depends on COMPARE64
    JUMP(0x56, 2), // Jump always

    EX16T8(0x57, 0), // Convert 16 bits to 8 bits
    EX32T8(0x58, 0), // Convert 32 bits to 8 bits
    EX64T8(0x59, 0), // Convert 64 bits to 8 bits

    EX8T16(0x5A, 0), // Extend 8 bits to 16 bits
    EX32T16(0x5B, 0), // Convert 32 bits to 16 bits
    EX64T16(0x5C, 0), // Convert 64 bits to 16 bits

    EX8T32(0x5D, 0), // Extend 8 bits to 32 bits
    EX16T32(0x5E, 0), // Extend 16 bits to 32 bits
    EX64T32(0x5F, 0), // Convert 64 bits to 32 bits

    EX8T64(0x60, 0), // Extend 8 bits to 64 bits
    EX16T64(0x61, 0), // Extend 16 bits to 64 bits
    EX32T64(0x62, 0), // Extend 32 bits to 64 bits

    ;

    private final int id;
    private final int operandSize;

    BytecodeInstructions(int id, int operandSize) {
        this.id = id;
        this.operandSize = operandSize;
    }

    public static BytecodeInstructions fromOpcode(int opcode) {
        for(BytecodeInstructions instr : values()) {
            if(instr.id == opcode) {
                return instr;
            }
        }
        throw new RuntimeException("Bytecode opcode " + opcode + " does not exist");
    }

    public int getId() {
        return id;
    }

    public int getOperandSize() {
        return operandSize;
    }
}
