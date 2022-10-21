package ga.epicpix.zprol.parser.tree;

public final class AccessorTree implements IExpression {
    private final int start;
    private final int end;
    public final IAccessorElement[] accessorElements;

    public AccessorTree(int start, int end, IAccessorElement[] accessorElements) {
        this.start = start;
        this.end = end;
        this.accessorElements = accessorElements;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
