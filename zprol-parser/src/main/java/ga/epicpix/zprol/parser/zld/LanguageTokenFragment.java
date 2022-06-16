package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.utils.SeekIterator;

import java.util.function.Function;

public class LanguageTokenFragment {

    private final Function<SeekIterator<LexerToken>, Token[]> tokenReader;
    private final String debugName;

    protected LanguageTokenFragment(Function<SeekIterator<LexerToken>, Token[]> tokenReader, String debugName) {
        this.tokenReader = tokenReader;
        this.debugName = debugName;
    }

    public Token[] apply(SeekIterator<LexerToken> tokens) {
        return tokenReader.apply(tokens);
    }

    public String getDebugName() {
        return debugName;
    }

    public String toString() {
        return debugName;
    }
}
