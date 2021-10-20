package ga.epicpix.zprol.tokens;

public class OperatorToken extends Token {

    private final String operator;

    public OperatorToken(String operator) {
        super(TokenType.OPERATOR);
        this.operator = operator;
    }

    protected String getData() {
        return "\"" + operator + "\"";
    }
}
