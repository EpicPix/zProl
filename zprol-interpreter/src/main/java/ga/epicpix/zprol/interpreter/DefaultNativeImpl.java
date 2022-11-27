package ga.epicpix.zprol.interpreter;

import ga.epicpix.zpil.GeneratedData;
import ga.epicpix.zprol.structures.Function;
import ga.epicpix.zprol.types.PrimitiveType;
import ga.epicpix.zprol.types.Type;

import java.util.Arrays;
import java.util.Objects;

public class DefaultNativeImpl extends NativeImpl {

    private long runSyscall(VMState state, Object num, Object arg0, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
        if(num == Long.valueOf(1) /* SYS_WRITE */) {
            if(arg0 instanceof Long) {
                Long alg0 = (Long) arg0;
                if(!(arg1 instanceof byte[] || arg1 instanceof Long)) {
                    throw new RuntimeException("Expected syscall arg1 to be a byte array of characters");
                }
                if(!(arg2 instanceof Long)) {
                    throw new RuntimeException("Expected syscall arg2 to be a number which has the amount of characters");
                }
                if(arg1 instanceof Long) {
                    arg1 = state.memory.cloneBytes((Long) arg1, (Long) arg2);
                }
                if(alg0 == 0 /* STDOUT */) {
                    System.out.write((byte[]) arg1, 0, Math.toIntExact((Long) arg2));
                    return Math.toIntExact((Long) arg2);
                }else if(alg0 == 1 /* STDERR */) {
                    System.err.write((byte[]) arg1, 0, Math.toIntExact((Long) arg2));
                    return Math.toIntExact((Long) arg2);
                }else {
                    throw new RuntimeException("Unknown file descriptor, only STDOUT (0) and STDERR (1) are supported");
                }
            }else {
                throw new RuntimeException("Expected syscall arg0 to be a number");
            }
        }else if(num == Long.valueOf(9) /* SYS_MMAP */) {
            if(state.memory instanceof DefaultMemoryImpl) {
                // TODO finish mmap properly
                DefaultMemoryImpl dmi = (DefaultMemoryImpl) state.memory;
                return dmi.registerMemory((Long) arg0, (Long) arg1);
            }else {
                throw new RuntimeException("Cannot use the mmap syscall with a non-default memory implementation");
            }
        }else if(num == Long.valueOf(11) /* SYS_MUNMAP */) {
            if(state.memory instanceof DefaultMemoryImpl) {
                DefaultMemoryImpl dmi = (DefaultMemoryImpl) state.memory;
                return dmi.unregisterMemory((Long) arg0) ? 0L : -1L;
            }else {
                throw new RuntimeException("Cannot use the munmap syscall with a non-default memory implementation");
            }
        }
        throw new RuntimeException("Cannot handle syscall: " + num + " / " + arg0 + " / " + arg1 + " / " + arg2 + " / " + arg3 + " / " + arg4 + " / " + arg5);
    }

    public Object runNativeFunction(GeneratedData file, VMState state, LocalStorage locals) {
        Function current = state.currentFunction();
        if(Objects.equals(current.namespace, "zprol.lang.linux.amd64")) {
            if(current.name.equals("syscall")) {
                switch(current.signature.toString()) {
                    case "uL(L)":
                        return runSyscall(state, locals.getLongValue(8), 0, 0, 0, 0, 0, 0);
                    case "uL(LL)":
                        return runSyscall(state, locals.getLongValue(8), locals.getLongValue(16), 0, 0, 0, 0, 0);
                    case "uL(LLL)":
                        return runSyscall(state, locals.getLongValue(8), locals.getLongValue(16), locals.getLongValue(24), 0, 0, 0, 0);
                    case "uL(LLLL)":
                        return runSyscall(state, locals.getLongValue(8), locals.getLongValue(16), locals.getLongValue(24), locals.getLongValue(32), 0, 0, 0);
                    case "uL(LLLLL)":
                        return runSyscall(state, locals.getLongValue(8), locals.getLongValue(16), locals.getLongValue(24), locals.getLongValue(32), locals.getLongValue(40), 0, 0);
                    case "uL(LLLLLL)":
                        return runSyscall(state, locals.getLongValue(8), locals.getLongValue(16), locals.getLongValue(24), locals.getLongValue(32), locals.getLongValue(40), locals.getLongValue(48), 0);
                    case "uL(LLLLLLL)":
                        return runSyscall(state, locals.getLongValue(8), locals.getLongValue(16), locals.getLongValue(24), locals.getLongValue(32), locals.getLongValue(40), locals.getLongValue(48), locals.getLongValue(56));
                    default:
                }
            }
        }
        Object[] extracted = new Object[current.signature.parameters.length];
        int loc = 0;
        Type[] parameters = current.signature.parameters;
        for(int i = 0; i < parameters.length; i++) {
            Type param = parameters[i];
            int size;
            if(param instanceof PrimitiveType) {
                PrimitiveType prim = (PrimitiveType) param;
                size = prim.size;
            } else size = 8;
            loc += size;
            extracted[i] = locals.get(loc, size).value;
        }
        throw new RuntimeException("Unknown native method: " + current + " called with " + Arrays.toString(extracted));
    }

}
