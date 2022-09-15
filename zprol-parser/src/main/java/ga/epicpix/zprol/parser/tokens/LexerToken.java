package ga.epicpix.zprol.parser.tokens;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.ParserLocation;

public final class LexerToken extends Token {

    private final int start;
    private final int end;

    public final String data;

    public LexerToken(TokenType type, String data, int start, int end, DataParser parser) {
        super(type, parser);
        this.start = start;
        this.end = end;
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public String toStringRaw() {
        return data;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}
