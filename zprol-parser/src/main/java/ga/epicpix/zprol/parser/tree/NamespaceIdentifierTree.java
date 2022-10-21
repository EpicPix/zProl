package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.tokens.LexerToken;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class NamespaceIdentifierTree implements ITree {
    private final int start;
    private final int end;
    public final LexerToken[] locations;

    public NamespaceIdentifierTree(int start, int end, LexerToken[] locations) {
        this.start = start;
        this.end = end;
        this.locations = locations;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

    public String toString() {
        return Arrays.stream(locations).map(x -> x.data).collect(Collectors.joining("."));
    }
}
