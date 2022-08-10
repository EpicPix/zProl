package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;

public record UsingTree(int start, int end, NamespaceIdentifierTree identifier) implements IDeclaration {
    public UsingTree(NamespaceIdentifierTree identifier) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), identifier);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}