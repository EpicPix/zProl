package ga.epicpix.zprol.parser.tree;

public final class ArrayAccessTree implements IAccessorElement {
    private final int start;
    private final int end;
    public final IExpression index;

    public ArrayAccessTree(int start, int end, IExpression index) {
        this.start = start;
        this.end = end;
        this.index = index;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
