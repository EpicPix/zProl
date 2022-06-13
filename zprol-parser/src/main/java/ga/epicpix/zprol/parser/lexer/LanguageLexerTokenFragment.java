package ga.epicpix.zprol.parser.lexer;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.tokens.Token;

import java.util.function.Function;

public class LanguageLexerTokenFragment {

    private final Function<DataParser, String> tokenReader;
    private final String debugName;

    protected LanguageLexerTokenFragment(Function<DataParser, String> tokenReader, String debugName) {
        this.tokenReader = tokenReader;
        this.debugName = debugName;
    }

    public String apply(DataParser parser) {
        return tokenReader.apply(parser);
    }

    public Function<DataParser, String> getTokenReader() {
        return tokenReader;
    }

    public String getDebugName() {
        return debugName;
    }

    public String toString() {
        return debugName;
    }

}
