package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;
import ga.epicpix.zprol.parser.tokens.LexerToken;

public record LiteralTree(int start, int end, LiteralType type, Object value) implements IExpression {
    public LiteralTree(LiteralType type, Object value) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), type, value);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}

