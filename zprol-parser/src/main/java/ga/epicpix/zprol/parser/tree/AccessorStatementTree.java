package ga.epicpix.zprol.parser.tree;

public record AccessorStatementTree(int start, int end, AccessorTree accessor) implements IStatement {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
