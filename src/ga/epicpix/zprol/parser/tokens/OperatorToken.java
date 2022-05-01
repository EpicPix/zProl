package ga.epicpix.zprol.parser.tokens;

import ga.epicpix.zprol.zld.LanguageOperator;

public class OperatorToken extends Token {

    public final LanguageOperator operator;

    public OperatorToken(LanguageOperator operator) {
        super(TokenType.OPERATOR);
        this.operator = operator;
    }

    protected String getData() {
        return "\"" + operator.operator() + "\"";
    }
}
