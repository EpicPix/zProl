package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;

public record WhileStatementTree(int start, int end, IExpression expression, CodeTree code) implements IStatement {
    public WhileStatementTree(IExpression expression, CodeTree code) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), expression, code);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
