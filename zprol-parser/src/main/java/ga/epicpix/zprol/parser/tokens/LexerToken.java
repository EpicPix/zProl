package ga.epicpix.zprol.parser.tokens;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.ParserLocation;

public final class LexerToken extends Token {

    private final int start;
    private final int end;

    public final String data;

    public LexerToken(String name, String data, int start, int end, DataParser parser) {
        super(name, parser);
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

    public ParserLocation getStartLocation() {
        return parser.getLocation(start);
    }

    public ParserLocation getEndLocation() {
        return parser.getLocation(end);
    }
}
