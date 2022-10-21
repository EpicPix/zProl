package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.tokens.LexerToken;

public final class OperatorExpressionTree implements IExpression {
    private final int start;
    private final int end;
    public final IExpression left;
    public final LexerToken operator;
    public final IExpression right;

    public OperatorExpressionTree(int start, int end, IExpression left, LexerToken operator, IExpression right) {
        this.start = start;
        this.end = end;
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
