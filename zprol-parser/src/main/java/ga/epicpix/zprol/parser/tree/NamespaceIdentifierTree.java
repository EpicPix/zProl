package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.tokens.LexerToken;

public record NamespaceIdentifierTree(int start, int end, LexerToken[] locations) implements ITree {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
