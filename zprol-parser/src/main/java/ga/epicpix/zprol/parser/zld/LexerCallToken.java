package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.utils.SeekIterator;

import java.util.function.Function;

class LexerCallToken extends LanguageTokenFragment {

    LexerCallToken(String use) {
        super(new LexerCallTokenTokenReader(use), "$$" + use);
    }


    public record LexerCallTokenTokenReader(String use) implements Function<SeekIterator<LexerToken>, Token[]> {
        public Token[] apply(SeekIterator<LexerToken> tokens) {
            if (!tokens.hasNext()) {
                return null;
            }
            if (!tokens.seek().name.equals(use)) {
                return null;
            }
            return new Token[]{tokens.next()};
        }
    }
}
