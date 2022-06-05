package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.LanguageKeyword;
import ga.epicpix.zprol.parser.exceptions.ParserException;
import ga.epicpix.zprol.parser.tokens.KeywordToken;
import ga.epicpix.zprol.parser.tokens.Token;

import java.util.function.Function;

class ExactKeywordTokenReader implements Function<DataParser, Token[]> {

    public final LanguageKeyword keyword;

    ExactKeywordTokenReader(String keyword, DataParser location) {
        this.keyword = LanguageKeyword.KEYWORDS.get(keyword);
        if(this.keyword == null) {
            throw new ParserException("Unknown language keyword", location);
        }
    }

    public Token[] apply(DataParser parser) {
        parser.ignoreWhitespace();
        var start = parser.getLocation();
        if(!keyword.keyword().equals(parser.nextWord())) {
            return null;
        }
        return new Token[] {new KeywordToken(keyword, start, parser.getLocation(), parser)};
    }
}
