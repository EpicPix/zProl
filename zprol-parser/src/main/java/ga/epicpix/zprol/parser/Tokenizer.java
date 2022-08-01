package ga.epicpix.zprol.parser;

import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.utils.SeekIterator;

import java.util.ArrayList;

public class Tokenizer {

    private static boolean skipWhitespace(SeekIterator<LexerToken> lexerTokens) {
        while(lexerTokens.hasNext() && lexerTokens.seek().name.equals("Whitespace")) {
            lexerTokens.next();
        }
        return lexerTokens.hasNext();
    }

    public static ArrayList<Token> tokenize(SeekIterator<LexerToken> lexerTokens) {
        var tokens = new ArrayList<Token>();
        while(skipWhitespace(lexerTokens)) {
            var next = lexerTokens.next();
            throw new TokenLocatedException("Unexpected token", next);
        }
        return tokens;
    }

}
