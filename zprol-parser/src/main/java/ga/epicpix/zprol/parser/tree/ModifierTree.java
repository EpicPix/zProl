package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;

public record ModifierTree(int start, int end, long mod) implements ITree {

    public static final int NATIVE = 1 << 0;

    public ModifierTree(long mod) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), mod);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
