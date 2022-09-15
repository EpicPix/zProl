package ga.epicpix.zprol.parser.tree;

public record ArgumentsTree(int start, int end, IExpression[] arguments) implements ITree {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
