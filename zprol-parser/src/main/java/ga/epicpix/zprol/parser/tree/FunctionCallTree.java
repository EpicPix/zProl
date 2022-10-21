package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.tokens.LexerToken;

public final class FunctionCallTree implements IStatement, IAccessorElement {
    private final int start;
    private final int end;
    public final LexerToken name;
    public final ArgumentsTree arguments;

    public FunctionCallTree(int start, int end, LexerToken name, ArgumentsTree arguments) {
        this.start = start;
        this.end = end;
        this.name = name;
        this.arguments = arguments;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
