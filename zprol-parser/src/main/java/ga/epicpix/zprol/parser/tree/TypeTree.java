package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.tokens.LexerToken;

import java.util.Objects;

public final class TypeTree implements ITree {
    private final int start;
    private final int end;
    public final LexerToken type;
    public final int arrayAmount;

    public TypeTree(int start, int end, LexerToken type, int arrayAmount) {
        this.start = start;
        this.end = end;
        this.type = type;
        this.arrayAmount = arrayAmount;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder(type.data);
        for(int i = 0; i<arrayAmount; i++) builder.append("[]");
        return builder.toString();
    }
}

