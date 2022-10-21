package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.tokens.LexerToken;

public final class FieldTree implements IDeclaration {
    private final int start;
    private final int end;
    public final boolean isConst;
    public final TypeTree type;
    public final LexerToken name;
    public final IExpression value;

    public FieldTree(int start, int end, boolean isConst, TypeTree type, LexerToken name, IExpression value) {
        this.start = start;
        this.end = end;
        this.isConst = isConst;
        this.type = type;
        this.name = name;
        this.value = value;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
