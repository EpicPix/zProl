package ga.epicpix.zprol.parser;

import ga.epicpix.zprol.parser.tokens.LexerToken;

import java.util.ArrayList;

public class LexerResults {

    public final ArrayList<LexerToken> tokens;
    public final DataParser parser;

    public LexerResults(ArrayList<LexerToken> tokens, DataParser parser) {
        this.tokens = tokens;
        this.parser = parser;
    }
}
