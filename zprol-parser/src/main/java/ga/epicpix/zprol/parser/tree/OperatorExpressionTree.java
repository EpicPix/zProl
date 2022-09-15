package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.tokens.LexerToken;

public record OperatorExpressionTree(int start, int end, IExpression left, LexerToken operator, IExpression right) implements IExpression {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
