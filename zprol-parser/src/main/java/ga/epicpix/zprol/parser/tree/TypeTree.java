package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;
import ga.epicpix.zprol.parser.tokens.LexerToken;

public record TypeTree(int start, int end, LexerToken type, int arrayAmount) implements ITree {
    public TypeTree(LexerToken type, int arrayAmount) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), type, arrayAmount);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

    public String toString() {
        return type.name + "[]".repeat(arrayAmount);
    }
}

