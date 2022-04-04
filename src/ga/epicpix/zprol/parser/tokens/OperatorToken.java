package ga.epicpix.zprol.parser.tokens;

public class OperatorToken extends Token {

    public final String operator;

    public OperatorToken(String operator) {
        super(TokenType.OPERATOR);
        this.operator = operator;
    }

    protected String getData() {
        return "\"" + operator + "\"";
    }
}