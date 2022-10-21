package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.tokens.LexerToken;

public final class CreateAssignmentStatementTree implements IStatement {
    private final int start;
    private final int end;
    public final TypeTree type;
    public final LexerToken name;
    public final IExpression expression;

    public CreateAssignmentStatementTree(int start, int end, TypeTree type, LexerToken name, IExpression expression) {
        this.start = start;
        this.end = end;
        this.type = type;
        this.name = name;
        this.expression = expression;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
