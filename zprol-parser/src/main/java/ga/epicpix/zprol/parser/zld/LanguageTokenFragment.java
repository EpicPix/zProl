package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.LanguageKeyword;
import ga.epicpix.zprol.parser.exceptions.ParserException;
import ga.epicpix.zprol.parser.tokens.KeywordToken;
import ga.epicpix.zprol.parser.tokens.Token;

import java.util.Arrays;
import java.util.function.Function;

public class LanguageTokenFragment {

    private final Function<DataParser, Token[]> tokenReader;
    private final String debugName;

    LanguageTokenFragment(Function<DataParser, Token[]> tokenReader, String debugName) {
        this.tokenReader = tokenReader;
        this.debugName = debugName;
    }

    private static final boolean hasDebugLogging = Boolean.parseBoolean(System.getProperty("TOKEN_LOG"));
    private static int indent = 0;

    public Token[] apply(DataParser parser) {
        if(!hasDebugLogging) {
            return tokenReader.apply(parser);
        }
        indent++;
        System.out.println(" ".repeat(indent) + "Start " + getDebugName().replace("\n", "\\n"));
        var a = tokenReader.apply(parser);
        System.out.println(" ".repeat(indent) + "End " + getDebugName().replace("\n", "\\n") + "     " + Arrays.toString(a).replace("\n", "\\n"));
        indent--;
        return a;
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
