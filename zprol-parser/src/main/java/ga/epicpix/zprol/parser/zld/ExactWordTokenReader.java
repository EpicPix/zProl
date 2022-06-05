package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.WordToken;

import java.util.function.Function;

import static ga.epicpix.zprol.parser.zld.CallToken.EMPTY_TOKENS;

class ExactWordTokenReader implements Function<DataParser, Token[]> {

    public final String require;

    ExactWordTokenReader(String require) {
        this.require = require;
    }

    public Token[] apply(DataParser parser) {
        var startLocation = parser.getLocation();
        String got = parser.nextWord();
        if(!require.equals(got)) {
            return null;
        }
        var endLocation = parser.getLocation();
        return new Token[]{new WordToken(got, startLocation, endLocation, parser)};
    }
}
