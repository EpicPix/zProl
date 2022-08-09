package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;
import ga.epicpix.zprol.parser.tokens.LexerToken;

public record FunctionCallTree(int start, int end, LexerToken name, ArgumentsTree arguments) implements IStatement, IAccessorElement {
    public FunctionCallTree(LexerToken name, ArgumentsTree arguments) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), name, arguments);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
