package ga.epicpix.zprol.parser.tokens;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.ParserLocation;

public class Token {

    public final ParserLocation startLocation;
    public final ParserLocation endLocation;
    public final DataParser parser;
    public final String name;

    public Token(String name, ParserLocation startLocation, ParserLocation endLocation, DataParser parser) {
        this.name = name;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.parser = parser;
    }

    public NamedToken asNamedToken() {
        return (NamedToken) this;
    }

    public LexerToken asLexerToken() {
        return (LexerToken) this;
    }

    protected String getData() {
        return "";
    }

    public String toString() {
        String data = getData();
        if(data.isEmpty()) {
            return getClass().getSimpleName();
        }
        return getClass().getSimpleName() + "(" + data + ")";
    }

    public String toStringRaw() {
        return getData();
    }
}
