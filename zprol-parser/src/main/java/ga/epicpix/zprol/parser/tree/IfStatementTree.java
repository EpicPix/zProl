package ga.epicpix.zprol.parser.tree;

public record IfStatementTree(int start, int end, IExpression expression, CodeTree code, ElseStatementTree elseStatement) implements IStatement {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
