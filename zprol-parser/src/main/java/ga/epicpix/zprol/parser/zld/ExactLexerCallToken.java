package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.lexer.LanguageLexerToken;
import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.utils.SeekIterator;

import java.util.function.Function;

class ExactLexerCallToken extends LanguageTokenFragment {

    ExactLexerCallToken(LanguageLexerToken token, String name) {
        super(new LexerCallTokenTokenReader(token), "'" + name + "'");
    }

    public record LexerCallTokenTokenReader(LanguageLexerToken token) implements Function<SeekIterator<LexerToken>, Token[]> {
        public Token[] apply(SeekIterator<LexerToken> tokens) {
            if (!tokens.hasNext()) {
                return null;
            }
            if (tokens.seek().lToken != token) {
                return null;
            }
            return new Token[]{tokens.next()};
        }
    }
}
