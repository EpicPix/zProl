package ga.epicpix.zprol.parser.tokens;

import ga.epicpix.zprol.zld.LanguageKeyword;

public class KeywordToken extends Token implements WordHolder {

    private final LanguageKeyword keyword;

    public KeywordToken(LanguageKeyword keyword) {
        super(TokenType.KEYWORD);
        this.keyword = keyword;
    }

    public String getWord() {
        return keyword.keyword();
    }

    protected String getData() {
        return keyword.keyword();
    }
}
