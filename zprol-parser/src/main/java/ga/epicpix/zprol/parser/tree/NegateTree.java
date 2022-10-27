package ga.epicpix.zprol.parser.tree;

public final class NegateTree implements IExpression {
    private final int start;
    private final int end;
    public final IExpression expression;

    public NegateTree(int start, int end, IExpression expression) {
        this.start = start;
        this.end = end;
        this.expression = expression;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
