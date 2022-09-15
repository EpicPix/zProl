package ga.epicpix.zprol.parser.tree;

public record ElseStatementTree(int start, int end, CodeTree code) implements IStatement {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
