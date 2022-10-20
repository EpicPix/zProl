package ga.epicpix.zprol.interpreter;

import java.util.ArrayList;
import java.util.List;

class LocalStorage {

    private final List<LocalValue> values = new ArrayList<>();

    public void set(Object value, int loc, int size) {
        for(int i = 0; i<values.size(); i++) {
            var v = values.get(i);
            if((v.index() < loc && loc <= v.index() + v.size()) || (v.index() < loc + size && loc + size <= v.index() + v.size())) {
                values.remove(i--);
            }
        }
        values.add(new LocalValue(value, loc, size));
    }

    public LocalValue get(int loc, int size) {
        if(size < 0) throw new IllegalArgumentException("Cannot get negative bytes");
        if(size > 8) throw new IllegalArgumentException("Cannot get more than 8 bytes");
        for(LocalValue v : values) {
            if(v.index() == loc) {
                if(v.size() != size)
                    throw new IllegalStateException("Expected " + size + " bytes, but got " + v.size() + " bytes");
                return v;
            }
        }
        throw new IllegalStateException("Undefined local data at " + loc);
    }

}
