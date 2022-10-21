package ga.epicpix.zprol.parser.tree;

public final class WhileStatementTree implements IStatement {
    private final int start;
    private final int end;
    public final IExpression expression;
    public final CodeTree code;

    public WhileStatementTree(int start, int end, IExpression expression, CodeTree code) {
        this.start = start;
        this.end = end;
        this.expression = expression;
        this.code = code;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
