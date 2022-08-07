package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;

public record ModifiersTree(int start, int end, ModifierTree[] modifiers) implements ITree {

    public ModifiersTree(ModifierTree[] modifiers) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), modifiers);
    }

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
