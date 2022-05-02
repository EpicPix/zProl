package ga.epicpix.zprol.zld;

import ga.epicpix.zprol.exceptions.ParserException;
import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.tokens.KeywordToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.WordToken;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

public class LanguageTokenFragment {

    private final Function<DataParser, Token[]> tokenReader;
    private final String debugName;

    public static WordToken exactWordGenerator(String require, String got) {
        if(!require.equals(got)) {
            return null;
        }
        return new WordToken(got);
    }

    public static String validateWord(String w) {
        return switch(w) {
            case ";", ",", "(", ")", "{", "}" -> null;
            default -> w;
        };
    }

    private LanguageTokenFragment(Function<DataParser, Token[]> tokenReader, String debugName) {
        this.tokenReader = tokenReader;
        this.debugName = debugName;
    }

    public static LanguageTokenFragment createExactFragmentType(String data, Supplier<Token> gen) {
        final Token token = gen.get();
        return new LanguageTokenFragment(p -> exactWordGenerator(data, p.nextWord()) != null ? new Token[] {token} : null, data);
    }

    public static LanguageTokenFragment createExactFragment(String data) {
        return new LanguageTokenFragment(p -> {
            var e = exactWordGenerator(data, p.nextWord());
            return e != null ? new Token[] {e} : null;
        }, data);
    }

    public static LanguageTokenFragment createExactKeywordFragment(String data, DataParser parser) {
        LanguageKeyword keyword = Language.KEYWORDS.get(data);
        if(keyword == null) {
            throw new ParserException("Unknown language keyword", parser);
        }
        return new LanguageTokenFragment(p -> {
            if(!data.equals(p.nextWord())) {
                return null;
            }
            return new Token[] {new KeywordToken(keyword)};
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

    public Token[] apply(DataParser parser) {
        return tokenReader.apply(parser);
    }

    public String getDebugName() {
        return debugName;
    }

    public String toString() {
        return debugName;
    }
}
