package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.LanguageKeyword;
import ga.epicpix.zprol.parser.exceptions.ParserException;
import ga.epicpix.zprol.parser.tokens.KeywordToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.TokenType;
import ga.epicpix.zprol.parser.tokens.WordToken;

import java.util.Arrays;
import java.util.function.Function;

public class LanguageTokenFragment {

    private final Function<DataParser, Token[]> tokenReader;
    private final String debugName;

    public static WordToken exactWordGenerator(String require, DataParser parser) {
        var startLocation = parser.getLocation();
        String got = parser.nextWord();
        if(!require.equals(got)) {
            return null;
        }
        var endLocation = parser.getLocation();
        return new WordToken(got, startLocation, endLocation, parser);
    }

    public static Token exactTypeGenerator(String require, TokenType type, DataParser parser) {
        var startLocation = parser.getLocation();
        String got = parser.nextWord();
        if(!require.equals(got)) {
            return null;
        }
        var endLocation = parser.getLocation();
        return new Token(type, startLocation, endLocation, parser);
    }

    LanguageTokenFragment(Function<DataParser, Token[]> tokenReader, String debugName) {
        this.tokenReader = tokenReader;
        this.debugName = debugName;
    }

    public static LanguageTokenFragment createExactFragment(String data) {
        return new LanguageTokenFragment(p -> {
            var e = exactWordGenerator(data, p);
            return e != null ? new Token[] {e} : null;
        }, data);
    }

    public static LanguageTokenFragment createExactKeywordFragment(String data, DataParser parser) {
        LanguageKeyword keyword = LanguageKeyword.KEYWORDS.get(data);
        if(keyword == null) {
            throw new ParserException("Unknown language keyword", parser);
        }
        return new LanguageTokenFragment(p -> {
            p.ignoreWhitespace();
            var start = p.getLocation();
            if(!data.equals(p.nextWord())) {
                return null;
            }
            return new Token[] {new KeywordToken(keyword, start, p.getLocation(), p)};
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

    public String getDebugName() {
        return debugName;
    }

    public String toString() {
        return debugName;
    }
}
