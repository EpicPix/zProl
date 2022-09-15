package ga.epicpix.zprol.parser.tree;

public record ReturnStatementTree(int start, int end, IExpression expression) implements IStatement {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
