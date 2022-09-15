package ga.epicpix.zprol.parser.tree;

public record BreakStatementTree(int start, int end) implements IStatement {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
