package ga.epicpix.zprol.parser.tokens;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.ParserLocation;

public abstract class Token {

    public final DataParser parser;
    public final TokenType type;

    public Token(TokenType type, DataParser parser) {
        this.type = type;
        this.parser = parser;
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

    public abstract int getStart();
    public abstract int getEnd();

}
