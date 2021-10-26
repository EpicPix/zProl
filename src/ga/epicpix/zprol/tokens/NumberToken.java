package ga.epicpix.zprol.tokens;

import java.math.BigInteger;

public class NumberToken extends Token {

    public final BigInteger number;

    public NumberToken(BigInteger number) {
        super(TokenType.NUMBER);
        this.number = number;
    }

    protected String getData() {
        return number.toString();
    }
}
