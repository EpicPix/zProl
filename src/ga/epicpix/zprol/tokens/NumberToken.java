package ga.epicpix.zprol.tokens;

public class NumberToken extends Token {

    public final long number;

    public NumberToken(long number) {
        super(TokenType.NUMBER);
        this.number = number;
    }

    protected String getData() {
        return Long.toString(number);
    }
}
