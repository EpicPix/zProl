package ga.epicpix.zprol.parser.tree;

public final class CastTree implements IExpression {
    private final int start;
    private final int end;
    public final TypeTree type;
    public final boolean hardCast;
    public final IExpression value;

    public CastTree(int start, int end, TypeTree type, boolean hardCast, IExpression value) {
        this.start = start;
        this.end = end;
        this.type = type;
        this.hardCast = hardCast;
        this.value = value;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}

