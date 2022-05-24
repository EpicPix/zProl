package ga.epicpix.zprol.parser.tokens;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.LanguageKeyword;
import ga.epicpix.zprol.parser.ParserLocation;

public class KeywordToken extends Token implements WordHolder {

    private final LanguageKeyword keyword;

    public KeywordToken(LanguageKeyword keyword, ParserLocation startLocation, ParserLocation endLocation, DataParser parser) {
        super(TokenType.KEYWORD, startLocation, startLocation, parser);
        this.keyword = keyword;
    }

    public String getWord() {
        return keyword.keyword();
    }

    protected String getData() {
        return keyword.keyword();
    }
}
