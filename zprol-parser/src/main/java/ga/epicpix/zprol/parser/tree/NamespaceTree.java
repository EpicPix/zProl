package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.tokens.LexerToken;

public record NamespaceTree(int start, int end, LexerToken packageKeyword, NamespaceIdentifierTree identifier) implements ITree {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
