package ga.epicpix.zprol.parser.tokens;

public class KeywordToken extends Token implements WordHolder {

    private final String keyword;

    public KeywordToken(String keyword) {
        super(TokenType.KEYWORD);
        this.keyword = keyword;
    }

    public String getWord() {
        return keyword;
    }

    protected String getData() {
        return keyword;
    }
}
