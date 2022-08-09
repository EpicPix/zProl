package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;
import ga.epicpix.zprol.parser.tokens.LexerToken;

public record FieldAccessTree(int start, int end, LexerToken name) implements IAccessorElement {
    public FieldAccessTree(LexerToken name) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), name);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
