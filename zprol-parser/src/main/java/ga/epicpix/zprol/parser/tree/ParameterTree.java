package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.tokens.LexerToken;

public record ParameterTree(int start, int end, TypeTree type, LexerToken name) implements ITree {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
