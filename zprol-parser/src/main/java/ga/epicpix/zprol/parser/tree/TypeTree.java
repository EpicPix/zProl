package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.tokens.LexerToken;

public record TypeTree(int start, int end, LexerToken type, int arrayAmount) implements ITree {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

    public String toString() {
        return type.data + "[]".repeat(arrayAmount);
    }
}

