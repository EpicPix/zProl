package ga.epicpix.zprol.structures;

import java.util.EnumSet;

public enum FunctionModifiers {

    NATIVE(0x00000003, true, true),
    EMBED(0x00000002, false, true);

    public static final FunctionModifiers[] MODIFIERS = values();

    private final int bits;
    private final boolean emptyCode;
    private final boolean embedCode;

    FunctionModifiers(int bits, boolean emptyCode, boolean embedCode) {
        this.bits = bits;
        this.emptyCode = emptyCode;
        this.embedCode = embedCode;
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

    public boolean isEmbedCode() {
        return embedCode;
    }

    public static boolean isEmbedCode(EnumSet<FunctionModifiers> modifiers) {
        for(FunctionModifiers modifier : modifiers) {
            if(modifier.isEmbedCode()) {
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
