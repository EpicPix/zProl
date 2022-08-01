package ga.epicpix.zprol.parser.tokens;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.ParserLocation;

public abstract sealed class Token permits LexerToken, NamedToken {

    public final DataParser parser;
    public final String name;

    public Token(String name, DataParser parser) {
        this.name = name;
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

    public abstract ParserLocation getStartLocation();
    public abstract ParserLocation getEndLocation();
}
