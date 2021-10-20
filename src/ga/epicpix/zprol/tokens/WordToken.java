package ga.epicpix.zprol.tokens;

public class WordToken extends Token {

    private final String word;

    public WordToken(String word) {
        super(TokenType.WORD);
        this.word = word;
    }

    protected String getData() {
        return word;
    }
}
