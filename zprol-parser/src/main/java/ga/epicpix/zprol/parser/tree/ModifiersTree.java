package ga.epicpix.zprol.parser.tree;

public record ModifiersTree(int start, int end, ModifierTree[] modifiers) implements ITree {
    public long mods() {
        long m = 0;
        for(ModifierTree mod : modifiers) {
            m |= mod.mod();
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
