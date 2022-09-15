package ga.epicpix.zprol.parser.tree;

public record LiteralTree(int start, int end, LiteralType type, Object value) implements IExpression {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}

