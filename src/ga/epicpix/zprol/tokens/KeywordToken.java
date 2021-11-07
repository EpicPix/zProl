package ga.epicpix.zprol.tokens;

public class KeywordToken extends Token {

    public final String keyword;

    public KeywordToken(String keyword) {
        super(TokenType.KEYWORD);
        this.keyword = keyword;
    }

    protected String getData() {
        return keyword;
    }
}
