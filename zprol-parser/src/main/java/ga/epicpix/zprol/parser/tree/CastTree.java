package ga.epicpix.zprol.parser.tree;

public record CastTree(int start, int end, TypeTree type, boolean hardCast, IExpression value) implements IExpression {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}

