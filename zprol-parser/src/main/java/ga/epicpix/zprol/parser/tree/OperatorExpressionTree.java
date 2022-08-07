package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;
import ga.epicpix.zprol.parser.tokens.LexerToken;

public record OperatorExpressionTree(int start, int end, IExpression left, LexerToken operator, IExpression right) implements IExpression {
    public OperatorExpressionTree(IExpression left, LexerToken operator, IExpression right) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), left, operator, right);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
