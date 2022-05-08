package ga.epicpix.zprol.parser.tokens;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.ParserLocation;

public class WordToken extends Token implements WordHolder {

    private final String word;

    public WordToken(String word, ParserLocation startLocation, ParserLocation endLocation, DataParser parser) {
        super(TokenType.WORD, startLocation, endLocation, parser);
        this.word = word;
    }

    public String getWord() {
        return word;
    }

    protected String getData() {
        return word;
    }
}
