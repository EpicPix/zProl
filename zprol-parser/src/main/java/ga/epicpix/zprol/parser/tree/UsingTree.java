package ga.epicpix.zprol.parser.tree;

public record UsingTree(int start, int end, NamespaceIdentifierTree identifier) implements IDeclaration {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
