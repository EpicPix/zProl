package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;

public record ReturnStatementTree(int start, int end, IExpression expression) implements IStatement {
    public ReturnStatementTree(IExpression expression) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), expression);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
