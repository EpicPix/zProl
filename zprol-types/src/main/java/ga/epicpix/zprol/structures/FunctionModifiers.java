package ga.epicpix.zprol.structures;

import java.util.EnumSet;

public enum FunctionModifiers {

    NATIVE(0x00000001, true);

    public static final FunctionModifiers[] MODIFIERS = values();

    private final int bits;
    private final boolean emptyCode;

    FunctionModifiers(int bits, boolean emptyCode) {
        this.bits = bits;
        this.emptyCode = emptyCode;
    }

    public int getBits() {
        return bits;
    }

    public boolean isEmptyCode() {
        return emptyCode;
    }

    public static boolean isEmptyCode(EnumSet<FunctionModifiers> modifiers) {
        for(FunctionModifiers modifier : modifiers) {
            if(modifier.isEmptyCode()) {
                return true;
            }
        }
        return false;
    }

    public static EnumSet<FunctionModifiers> getModifiers(int id) {
        EnumSet<FunctionModifiers> modifiers = EnumSet.noneOf(FunctionModifiers.class);
        for(var modifier : MODIFIERS) {
            if((modifier.getBits() & id) == modifier.getBits()) {
                modifiers.add(modifier);
            }
        }
        return modifiers;
    }

    public static int toBits(EnumSet<FunctionModifiers> modifiers) {
        int i = 0;
        for(var modifier : modifiers) {
            i |= modifier.bits;
        }
        return i;
    }
    
}
