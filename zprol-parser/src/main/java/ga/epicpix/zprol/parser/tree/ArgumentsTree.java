package ga.epicpix.zprol.parser.tree;

public final class ArgumentsTree implements ITree {
    private final int start;
    private final int end;
    public final IExpression[] arguments;

    public ArgumentsTree(int start, int end, IExpression[] arguments) {
        this.start = start;
        this.end = end;
        this.arguments = arguments;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
