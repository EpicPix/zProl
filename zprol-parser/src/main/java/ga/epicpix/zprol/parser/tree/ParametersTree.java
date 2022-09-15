package ga.epicpix.zprol.parser.tree;

public record ParametersTree(int start, int end, ParameterTree[] parameters) implements ITree {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
