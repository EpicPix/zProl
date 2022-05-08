package ga.epicpix.zprol.parser.tokens;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.ParserLocation;

public class Token {

    public final ParserLocation startLocation;
    public final ParserLocation endLocation;
    public final DataParser parser;

    private final TokenType type;

    public Token(TokenType type, ParserLocation startLocation, ParserLocation endLocation, DataParser parser) {
        this.type = type;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.parser = parser;
    }

    public WordToken asWordToken() {
        return (WordToken) this;
    }

    public KeywordToken asKeywordToken() {
        return (KeywordToken) this;
    }

    public NamedToken asNamedToken() {
        return (NamedToken) this;
    }

    public WordHolder asWordHolder() {
        return (WordHolder) this;
    }

    public TokenType getType() {
        return type;
    }

    protected String getData() {
        return "";
    }

    public String toString() {
        String data = getData();
        if(data.isEmpty()) {
            return type.name().toLowerCase();
        }
        return type.name().toLowerCase() + "(" + data + ")";
    }
}
