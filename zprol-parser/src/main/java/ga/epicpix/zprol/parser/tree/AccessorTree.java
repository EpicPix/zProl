package ga.epicpix.zprol.parser.tree;

public record AccessorTree(int start, int end, IAccessorElement[] accessorElements) implements IExpression {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
