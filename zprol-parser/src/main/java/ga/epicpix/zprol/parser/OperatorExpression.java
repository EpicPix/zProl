package ga.epicpix.zprol.parser;

import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.parser.tree.IExpression;

public class OperatorExpression {

    public final IExpression expression;
    public final LexerToken operator;

    public OperatorExpression(LexerToken operator, IExpression expression) {
        this.expression = expression;
        this.operator = operator;
    }
}
