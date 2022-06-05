package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.TokenType;

import java.util.function.Function;

import static ga.epicpix.zprol.parser.zld.CallToken.EMPTY_TOKENS;

class ExactTypeTokenReader implements Function<DataParser, Token[]> {

    public final String require;
    public final TokenType type;

    ExactTypeTokenReader(String require, TokenType type) {
        this.require = require;
        this.type = type;
    }

    public Token[] apply(DataParser parser) {
        var startLocation = parser.getLocation();
        String got = parser.nextWord();
        if(!require.equals(got)) {
            return null;
        }
        var endLocation = parser.getLocation();
        return new Token[]{new Token(type, startLocation, endLocation, parser)};
    }
}
