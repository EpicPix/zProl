package ga.epicpix.zprol.interpreter;

import java.util.ArrayList;

public class DefaultMemoryImpl extends MemoryImpl {

    final ArrayList<MemoryData> maps = new ArrayList<>();

    public byte get(long addr) {
        for(int i = 0, mapsSize = maps.size(); i < mapsSize; i++) {
            MemoryData v = maps.get(i);
            if(addr >= v.start && addr < v.start + v.length) {
                return v.data[Math.toIntExact(addr - v.start)];
            }
        }
        throw new RuntimeException("Unregistered memory at address 0x" + Long.toHexString(addr));
    }

    public void set(long addr, byte val) {
        for(int i = 0, mapsSize = maps.size(); i < mapsSize; i++) {
            MemoryData v = maps.get(i);
            if(addr >= v.start && addr < v.start + v.length) {
                v.data[Math.toIntExact(addr - v.start)] = val;
                return;
            }
        }
        throw new RuntimeException("Unregistered memory at address 0x" + Long.toHexString(addr));
    }

    public long registerMemory(long addr, long length) {
        addr &= ~2047L;
        if(addr != 0) {
            MemoryData e = null;
            for(MemoryData m : maps) {
                if(m.start >= addr && m.start + m.length < addr + length) {
                    e = m;
                    break;
                }
            }
            if(e == null) {
                addr = (addr + 2047) & ~2047L;
                maps.add(new MemoryData(addr, length, new byte[Math.toIntExact(length)]));
                return addr;
            }
        }

        addr = Math.max(addr, 2048);
        for(MemoryData v : maps) {
            addr = Math.max(addr, v.start + v.length);
        }
        addr = (addr + 2047) & ~2047L;
        maps.add(new MemoryData(addr, length, new byte[Math.toIntExact(length)]));
        return addr;
    }

    public boolean unregisterMemory(long addr) {
        for(int i = 0; i < maps.size(); i++) {
            if(maps.get(i).start == addr) {
                maps.remove(i);
                return true;
            }
        }
        return false;
    }

}

class MemoryData {

    public final long start;
    public final long length;
    public final byte[] data;

    MemoryData(long start, long length, byte[] data) {
        this.start = start;
        this.length = length;
        this.data = data;
    }
}