package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.OperatorExpression;
import ga.epicpix.zprol.parser.tokens.LexerToken;

import java.util.List;

public final class OperatorExpressionTree implements IExpression {
    private final int start;
    private final int end;
    public final IExpression left;
    public final OperatorExpression[] operators;

    public OperatorExpressionTree(int start, int end, IExpression left, OperatorExpression[] operators) {
        this.start = start;
        this.end = end;
        this.left = left;
        this.operators = operators;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
