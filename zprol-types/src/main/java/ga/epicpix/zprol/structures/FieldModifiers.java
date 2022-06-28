package ga.epicpix.zprol.structures;

import java.util.EnumSet;

public enum FieldModifiers {

    CONST(0x00000001, true);

    public static final FieldModifiers[] MODIFIERS = values();

    private final int bits;
    private final boolean isConst;

    FieldModifiers(int bits, boolean isConst) {
        this.bits = bits;
        this.isConst = isConst;
    }

    public int getBits() {
        return bits;
    }

    public boolean isConst() {
        return isConst;
    }

    public static boolean isConst(EnumSet<FieldModifiers> modifiers) {
        for(FieldModifiers modifier : modifiers) {
            if(modifier.isConst()) {
                return true;
            }
        }
        return false;
    }

    public static EnumSet<FieldModifiers> getModifiers(int id) {
        EnumSet<FieldModifiers> modifiers = EnumSet.noneOf(FieldModifiers.class);
        for(var modifier : MODIFIERS) {
            if((modifier.getBits() & id) == modifier.getBits()) {
                modifiers.add(modifier);
            }
        }
        return modifiers;
    }

    public static int toBits(EnumSet<FieldModifiers> modifiers) {
        int i = 0;
        for(var modifier : modifiers) {
            i |= modifier.bits;
        }
        return i;
    }

}
