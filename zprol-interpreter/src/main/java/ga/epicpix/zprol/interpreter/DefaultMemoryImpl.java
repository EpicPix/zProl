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
        for(int i = 0, mapsSize = pointerMaps.size(); i < mapsSize; i++) {
            ObjectMemoryData v = pointerMaps.get(i);
            if(addr >= v.start && addr < v.start + (v.count << 3)) {
                throw new RuntimeException("Trying to access a pointer map address 0x" + Long.toHexString(addr));
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
        for(int i = 0, mapsSize = pointerMaps.size(); i < mapsSize; i++) {
            ObjectMemoryData v = pointerMaps.get(i);
            if(addr >= v.start && addr < v.start + (v.count << 3)) {
                throw new RuntimeException("Trying to access a pointer map address 0x" + Long.toHexString(addr));
            }
        }
        throw new RuntimeException("Unregistered memory at address 0x" + Long.toHexString(addr));
    }

    final ArrayList<ObjectMemoryData> pointerMaps = new ArrayList<>();

    public Long objectToPointer(ILocatable data) {
        if(data.getLocation() == -1) {
            long addr = 0x8000000000000000L;
            if(pointerMaps.size() != 0) {
                ObjectMemoryData last = pointerMaps.get(pointerMaps.size() - 1);
                if(last.data[Math.toIntExact(last.count - 1)] != null) {
                    addr = last.start + (last.count << 3);
                    pointerMaps.add(new ObjectMemoryData(addr, 4096, new ILocatable[4096]));
                }
            }else {
                ObjectMemoryData map = new ObjectMemoryData(addr, 4096, new ILocatable[4096]);
                pointerMaps.add(map);
                data.setLocation(addr);
                map.data[0] = data;
                return addr;
            }
            ObjectMemoryData last = pointerMaps.get(pointerMaps.size() - 1);
            for(long i = 0; i<last.count; i++) {
                if(last.data[Math.toIntExact(i)] == null) {
                    data.setLocation(last.start + (i << 3));
                    last.data[Math.toIntExact(i)] = data;
                    break;
                }
            }
        }
        return data.getLocation();
    }

    public ILocatable pointerToObject(long addr) {
        addr &= ~0x7L;
        for(int i = 0, mapsSize = pointerMaps.size(); i < mapsSize; i++) {
            ObjectMemoryData v = pointerMaps.get(i);
            if(addr >= v.start && addr < v.start + (v.count << 3)) {
                ILocatable data = v.data[Math.toIntExact((addr - v.start) >> 3)];
                if(data == null) {
                    throw new RuntimeException("Tried to access a pointer with no data 0x" + Long.toHexString(addr));
                }
                return data;
            }
        }
        for(int i = 0, mapsSize = maps.size(); i < mapsSize; i++) {
            MemoryData v = maps.get(i);
            if(addr >= v.start && addr < v.start + v.length) {
                throw new RuntimeException("Trying to access a normal memory address 0x" + Long.toHexString(addr));
            }
        }
        throw new RuntimeException("No mapped addresses found");
    }

    public long registerMemory(long addr, long length) {
        addr &= ~2047L;
        if((addr & 0x8000000000000000L) != 0) {
            addr = 0;
        }
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
                if((addr & 0x8000000000000000L) != 0) {
                    throw new RuntimeException("Could not allocate memory!");
                }
                maps.add(new MemoryData(addr, length, new byte[Math.toIntExact(length)]));
                return addr;
            }
        }

        addr = Math.max(addr, 2048);
        for(MemoryData v : maps) {
            addr = Math.max(addr, v.start + v.length);
        }
        addr = (addr + 2047) & ~2047L;
        if((addr & 0x8000000000000000L) != 0) {
            throw new RuntimeException("Could not allocate memory!");
        }
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

class ObjectMemoryData {

    public final long start;
    public final long count;
    public final ILocatable[] data;

    ObjectMemoryData(long start, long count, ILocatable[] data) {
        this.start = start;
        this.count = count;
        this.data = data;
    }
}