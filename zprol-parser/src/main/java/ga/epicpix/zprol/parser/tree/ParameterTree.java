package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.tokens.LexerToken;

public final class ParameterTree implements ITree {
    private final int start;
    private final int end;
    public final TypeTree type;
    public final LexerToken name;

    public ParameterTree(int start, int end, TypeTree type, LexerToken name) {
        this.start = start;
        this.end = end;
        this.type = type;
        this.name = name;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
