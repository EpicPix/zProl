package ga.epicpix.zprol.tokens;

public class LongWordToken extends Token {

    public final String word;

    public LongWordToken(String word) {
        super(TokenType.LONG_WORD);
        this.word = word;
    }

    protected String getData() {
        return word;
    }
}
