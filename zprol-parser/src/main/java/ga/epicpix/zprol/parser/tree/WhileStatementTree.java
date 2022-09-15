package ga.epicpix.zprol.parser.tree;

public record WhileStatementTree(int start, int end, IExpression expression, CodeTree code) implements IStatement {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
