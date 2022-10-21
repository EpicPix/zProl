package ga.epicpix.zprol.parser.tree;

public final class ElseStatementTree implements IStatement {
    private final int start;
    private final int end;
    public final CodeTree code;

    public ElseStatementTree(int start, int end, CodeTree code) {
        this.start = start;
        this.end = end;
        this.code = code;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
