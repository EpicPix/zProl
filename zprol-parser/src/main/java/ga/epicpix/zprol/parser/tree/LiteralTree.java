package ga.epicpix.zprol.parser.tree;

public final class LiteralTree implements IExpression {
    private final int start;
    private final int end;
    public final LiteralType type;
    public final Object value;

    public LiteralTree(int start, int end, LiteralType type, Object value) {
        this.start = start;
        this.end = end;
        this.type = type;
        this.value = value;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}

