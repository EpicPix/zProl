package ga.epicpix.zprol.parser.tree;

public final class IfStatementTree implements IStatement {
    private final int start;
    private final int end;
    public final IExpression expression;
    public final CodeTree code;
    public final ElseStatementTree elseStatement;

    public IfStatementTree(int start, int end, IExpression expression, CodeTree code, ElseStatementTree elseStatement) {
        this.start = start;
        this.end = end;
        this.expression = expression;
        this.code = code;
        this.elseStatement = elseStatement;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
