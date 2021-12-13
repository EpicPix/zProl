package ga.epicpix.zprol.tokens;

public class DotWordToken extends Token {

    public final String word;

    public DotWordToken(String word) {
        super(TokenType.DOT_WORD);
        this.word = word;
    }

    protected String getData() {
        return word;
    }
}
