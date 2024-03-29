package ga.epicpix.zprol.parser.tree;

public final class ModifiersTree implements ITree {
    private final int start;
    private final int end;
    public final ModifierTree[] modifiers;

    public ModifiersTree(int start, int end, ModifierTree[] modifiers) {
        this.start = start;
        this.end = end;
        this.modifiers = modifiers;
    }

    public long mods() {
        long m = 0;
        for(ModifierTree mod : modifiers) {
            m |= mod.mod;
        }
        return m;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
