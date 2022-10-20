package ga.epicpix.zprol.interpreter;

import ga.epicpix.zpil.GeneratedData;

import java.util.Objects;

public class DefaultNativeImpl extends NativeImpl {

    private long runSyscall(VMState state, Object num, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if(num == Long.valueOf(1) /* SYS_WRITE */) {
            if(arg0 instanceof Long alg0) {
                if(!(arg1 instanceof byte[])) {
                    throw new RuntimeException("Expected syscall arg1 to be a byte array of characters");
                }
                if(!(arg2 instanceof Long)) {
                    throw new RuntimeException("Expected syscall arg2 to be a number which has the amount of characters");
                }
                if(alg0 == 0 /* STDOUT */) {
                    System.out.write((byte[]) arg1, 0, Math.toIntExact((Long) arg2));
                    return Math.toIntExact((Long) arg2);
                }else if(alg0 == 1 /* STDERR */) {
                    System.err.write((byte[]) arg1, 0, Math.toIntExact((Long) arg2));
                    return Math.toIntExact((Long) arg2);
                }else {
                    throw new RuntimeException("Unknown file descriptor, only STDOUT (0) and STDERR (1) is supported");
                }
            }else {
                throw new RuntimeException("Expected syscall arg0 to be a number");
            }
        }
        throw new RuntimeException("Cannot handle syscall: " + num + " / " + arg0 + " / " + arg1 + " / " + arg2 + " / " + arg3 + " / " + arg4 + " / " + arg5);
    }

    public Object runNative(GeneratedData file, VMState state, LocalStorage locals) {
        var current = state.currentFunction();
        if(Objects.equals(current.namespace(), "zprol.lang.linux.amd64")) {
            if(current.name().equals("syscall")) {
                return switch(current.signature().toString()) {
                    case "uL(L)" -> runSyscall(state, locals.getLongValue(8), 0, 0, 0, 0, 0, 0);
                    case "uL(LL)" -> runSyscall(state, locals.getLongValue(8), locals.getLongValue(16), 0, 0, 0, 0, 0);
                    case "uL(LLL)" -> runSyscall(state, locals.getLongValue(8), locals.getLongValue(16), locals.getLongValue(24), 0, 0, 0, 0);
                    case "uL(LLLL)" -> runSyscall(state, locals.getLongValue(8), locals.getLongValue(16), locals.getLongValue(24), locals.getLongValue(32), 0, 0, 0);
                    case "uL(LLLLL)" -> runSyscall(state, locals.getLongValue(8), locals.getLongValue(16), locals.getLongValue(24), locals.getLongValue(32), locals.getLongValue(40), 0, 0);
                    case "uL(LLLLLL)" -> runSyscall(state, locals.getLongValue(8), locals.getLongValue(16), locals.getLongValue(24), locals.getLongValue(32), locals.getLongValue(40), locals.getLongValue(48), 0);
                    case "uL(LLLLLLL)" -> runSyscall(state, locals.getLongValue(8), locals.getLongValue(16), locals.getLongValue(24), locals.getLongValue(32), locals.getLongValue(40), locals.getLongValue(48), locals.getLongValue(56));
                    default -> throw new RuntimeException("Unknown native method: " + current);
                };
            }else {
                throw new RuntimeException("Unknown native method: " + current);
            }
        }else {
            throw new RuntimeException("Unknown native method: " + current);
        }
    }

}
