package ga.epicpix.zprol.parser.tokens;

import java.math.BigInteger;

public class NumberToken extends Token {

    public final BigInteger number;

    public NumberToken(long number) {
        super(TokenType.NUMBER);
        this.number = BigInteger.valueOf(number);
    }

    public NumberToken(BigInteger number) {
        super(TokenType.NUMBER);
        this.number = number;
    }

    protected String getData() {
        return number.toString();
    }
}
