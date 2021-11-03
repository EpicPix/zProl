package ga.epicpix.zprol.compiled;

import java.util.ArrayList;

public enum Flag {

    INTERNAL(0),
    NO_IMPLEMENTATION(1),
    STATIC(2),

    ;

    public final int bit;
    public final int mask;

    Flag(int id) {
        this.bit = id;
        this.mask = 1 << id;
    }

    public static ArrayList<Flag> fromBits(int bits) {
        ArrayList<Flag> flags = new ArrayList<>();
        for(Flag f : values()) {
            if((f.mask & bits) == f.mask) {
                flags.add(f);
            }
        }
        return flags;
    }
}
