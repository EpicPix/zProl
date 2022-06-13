package ga.epicpix.zprol.parser.tokens;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.ParserLocation;

public class LexerToken extends Token {

    public final String data;

    public LexerToken(String name, String data, ParserLocation startLocation, ParserLocation endLocation, DataParser parser) {
        super(name, startLocation, endLocation, parser);
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public String toStringRaw() {
        return data;
    }
}
