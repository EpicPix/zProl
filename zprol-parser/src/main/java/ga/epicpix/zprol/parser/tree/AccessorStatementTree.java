package ga.epicpix.zprol.parser.tree;

public final class AccessorStatementTree implements IStatement {
    private final int start;
    private final int end;
    public final AccessorTree accessor;

    public AccessorStatementTree(int start, int end, AccessorTree accessor) {
        this.start = start;
        this.end = end;
        this.accessor = accessor;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
