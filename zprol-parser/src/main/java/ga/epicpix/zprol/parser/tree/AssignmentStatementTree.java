package ga.epicpix.zprol.parser.tree;

public record AssignmentStatementTree(int start, int end, AccessorTree accessor, IExpression expression) implements IStatement {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
