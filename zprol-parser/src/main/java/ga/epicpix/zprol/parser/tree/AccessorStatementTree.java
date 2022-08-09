package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;
import ga.epicpix.zprol.parser.tokens.LexerToken;

public record AccessorStatementTree(int start, int end, AccessorTree accessor) implements IStatement {
    public AccessorStatementTree(AccessorTree accessor) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), accessor);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
