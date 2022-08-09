package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;

public record BreakStatementTree(int start, int end) implements IStatement {
    public BreakStatementTree() {
        this(ParserState.popStartLocation(), ParserState.getEndLocation());
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
