package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.tokens.LexerToken;

public final class FieldAccessTree implements IAccessorElement {
    private final int start;
    private final int end;
    public final LexerToken name;

    public FieldAccessTree(int start, int end, LexerToken name) {
        this.start = start;
        this.end = end;
        this.name = name;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
