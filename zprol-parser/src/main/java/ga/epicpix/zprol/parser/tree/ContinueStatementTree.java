package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;

public record ContinueStatementTree(int start, int end) implements IStatement {
    public ContinueStatementTree() {
        this(ParserState.popStartLocation(), ParserState.getEndLocation());
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
