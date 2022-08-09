package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;
import ga.epicpix.zprol.parser.tokens.LexerToken;

import java.util.Arrays;
import java.util.stream.Collectors;

public record NamespaceIdentifierTree(int start, int end, LexerToken[] locations) implements ITree {
    public NamespaceIdentifierTree(LexerToken[] locations) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), locations);
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
