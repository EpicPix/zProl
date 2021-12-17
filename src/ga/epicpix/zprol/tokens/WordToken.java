package ga.epicpix.zprol.tokens;

public class WordToken extends Token implements WordHolder {

    private final String word;

    public WordToken(String word) {
        super(TokenType.WORD);
        this.word = word;
    }

    public String getWord() {
        return word;
    }

    protected String getData() {
        return word;
    }
}
