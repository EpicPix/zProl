package ga.epicpix.zprol.structures;

import java.util.ArrayList;
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

    public static FunctionModifiers getModifier(long id) {
        ArrayList<FunctionModifiers> modifiers = new ArrayList<>();
        for(FunctionModifiers modifier : MODIFIERS) {
            if((modifier.getBits() & id) == modifier.getBits()) {
                modifiers.add(modifier);
            }
        }
        if(modifiers.size() != 1) {
            throw new RuntimeException("Unknown modifier: 0x" + Long.toHexString(id));
        }
        return modifiers.get(0);
    }

    public static EnumSet<FunctionModifiers> getModifiers(int id) {
        EnumSet<FunctionModifiers> modifiers = EnumSet.noneOf(FunctionModifiers.class);
        for(FunctionModifiers modifier : MODIFIERS) {
            if((modifier.getBits() & id) == modifier.getBits()) {
                modifiers.add(modifier);
            }
        }
        return modifiers;
    }

    public static int toBits(EnumSet<FunctionModifiers> modifiers) {
        int i = 0;
        for(FunctionModifiers modifier : modifiers) {
            i |= modifier.bits;
        }
        return i;
    }
    
}
