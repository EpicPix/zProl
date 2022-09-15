package ga.epicpix.zprol.parser.tree;

public record ContinueStatementTree(int start, int end) implements IStatement {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
