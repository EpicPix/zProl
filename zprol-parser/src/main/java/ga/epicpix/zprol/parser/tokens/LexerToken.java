package ga.epicpix.zprol.parser.tokens;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.ParserLocation;
import ga.epicpix.zprol.parser.lexer.LanguageLexerToken;

public class LexerToken extends Token {

    public final String data;
    public final LanguageLexerToken lToken;

    public LexerToken(String name, String data, LanguageLexerToken lToken, ParserLocation startLocation, ParserLocation endLocation, DataParser parser) {
        super(name, startLocation, endLocation, parser);
        this.data = data;
        this.lToken = lToken;
    }

    public String getData() {
        return data;
    }

    public String toStringRaw() {
        return data;
    }
}
