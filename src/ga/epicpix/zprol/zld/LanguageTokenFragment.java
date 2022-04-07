package ga.epicpix.zprol.zld;
import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.WordToken;

import java.util.function.Function;

public class LanguageTokenFragment {

    private final Function<DataParser, Token[]> tokenReader;
    private final String debugName;

    public static WordToken exactWordGenerator(String require, String got) {
        if(!require.equals(got)) {
            return null;
        }
        return new WordToken(got);
    }

    private LanguageTokenFragment(Function<DataParser, Token[]> tokenReader, String debugName) {
        this.tokenReader = tokenReader;
        this.debugName = debugName;
    }

    public static LanguageTokenFragment createExactFragment(String data) {
        return new LanguageTokenFragment(p -> {
            var e = exactWordGenerator(data, p.nextWord());
            return e != null ? new Token[] {e} : null;
        }, data);
    }

    public static LanguageTokenFragment createSingle(Function<DataParser, Token> tokenReader, String debugName) {
        return new LanguageTokenFragment(p -> {
            var e = tokenReader.apply(p);
            return e != null ? new Token[] {e} : null;
        }, debugName);
    }

    public static LanguageTokenFragment createMulti(Function<DataParser, Token[]> tokenReader, String debugName) {
        return new LanguageTokenFragment(tokenReader, debugName);
    }

    public Function<DataParser, Token[]> getTokenReader() {
        return tokenReader;
    }

    public String getDebugName() {
        return debugName;
    }

    public String toString() {
        return debugName;
    }
}
