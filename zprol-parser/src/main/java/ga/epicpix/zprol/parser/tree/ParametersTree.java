package ga.epicpix.zprol.parser.tree;

public final class ParametersTree implements ITree {
    private final int start;
    private final int end;
    public final ParameterTree[] parameters;

    public ParametersTree(int start, int end, ParameterTree[] parameters) {
        this.start = start;
        this.end = end;
        this.parameters = parameters;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
