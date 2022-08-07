package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;

public record ElseStatementTree(int start, int end, CodeTree code) implements IStatement {
    public ElseStatementTree(CodeTree code) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), code);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
