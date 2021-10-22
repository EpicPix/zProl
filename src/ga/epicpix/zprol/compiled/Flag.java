package ga.epicpix.zprol.compiled;

public enum Flag {

    INTERNAL(0),
    NO_IMPLEMENTATION(1),

    ;

    public final int bit;
    public final int mask;

    Flag(int id) {
        this.bit = id;
        this.mask = 1 << id;
    }

}
