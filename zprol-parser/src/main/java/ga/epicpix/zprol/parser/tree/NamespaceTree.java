package ga.epicpix.zprol.parser.tree;

public final class NamespaceTree implements IDeclaration {
    private final int start;
    private final int end;
    public final NamespaceIdentifierTree identifier;

    public NamespaceTree(int start, int end, NamespaceIdentifierTree identifier) {
        this.start = start;
        this.end = end;
        this.identifier = identifier;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
