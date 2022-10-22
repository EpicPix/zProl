package ga.epicpix.zprol.interpreter;

public abstract class MemoryImpl {

    public abstract byte get(long addr);

    public short getShort(long addr) {
        return (short) ((get(addr)&0xff) | ((get(addr + 1)&0xff) << 8));
    }

    public int getInt(long addr) {
        return (get(addr)&0xff) | ((get(addr + 1)&0xff) << 8) | ((get(addr + 2)&0xff) << 16) | ((get(addr + 3)&0xff) << 24);
    }

    public long getLong(long addr) {
        return (get(addr)&0xff) | ((get(addr + 1)&0xff) << 8) | ((get(addr + 2)&0xff) << 16) | (((long)get(addr + 3)&0xff) << 24) | (((long)get(addr + 4)&0xff) << 32) | (((long)get(addr + 5)&0xff) << 40) | (((long)get(addr + 6)&0xff) << 48) | (((long)get(addr + 7)&0xff) << 56);
    }

    public abstract void set(long addr, byte val);

    public void setShort(long addr, short val) {
        set(addr, (byte) (val & 0xff));
        set(addr + 1, (byte) ((val >>> 8) & 0xff));
    }

    public void setInt(long addr, int val) {
        set(addr, (byte) (val & 0xff));
        set(addr + 1, (byte) ((val >>> 8) & 0xff));
        set(addr + 2, (byte) ((val >>> 16) & 0xff));
        set(addr + 3, (byte) ((val >>> 24) & 0xff));
    }

    public void setLong(long addr, long val) {
        set(addr, (byte) (val & 0xff));
        set(addr + 1, (byte) ((val >>> 8) & 0xff));
        set(addr + 2, (byte) ((val >>> 16) & 0xff));
        set(addr + 3, (byte) ((val >>> 24) & 0xff));
        set(addr + 4, (byte) ((val >>> 32) & 0xff));
        set(addr + 5, (byte) ((val >>> 40) & 0xff));
        set(addr + 6, (byte) ((val >>> 48) & 0xff));
        set(addr + 7, (byte) ((val >>> 56) & 0xff));
    }

    public abstract Long objectToPointer(ILocatable data);
    public abstract ILocatable pointerToObject(long addr);
}
