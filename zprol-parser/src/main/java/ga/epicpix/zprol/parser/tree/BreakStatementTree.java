package ga.epicpix.zprol.parser.tree;

public final class BreakStatementTree implements IStatement {
    private final int start;
    private final int end;

    public BreakStatementTree(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
