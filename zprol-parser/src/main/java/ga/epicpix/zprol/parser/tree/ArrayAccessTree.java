package ga.epicpix.zprol.parser.tree;

public record ArrayAccessTree(int start, int end, IExpression index) implements IAccessorElement {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
