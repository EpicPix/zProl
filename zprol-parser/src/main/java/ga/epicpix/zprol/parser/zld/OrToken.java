package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.utils.SeekIterator;

import java.util.function.Function;

import static ga.epicpix.zprol.parser.zld.CallToken.EMPTY_TOKENS;

class OrToken extends LanguageTokenFragment {

    OrToken(LanguageTokenFragment left, LanguageTokenFragment right) {
        super(new OrTokenTokenReader(left, right), left.getDebugName() + "|" + right.getDebugName());
    }

    public record OrTokenTokenReader(LanguageTokenFragment left, LanguageTokenFragment right) implements Function<SeekIterator<LexerToken>, Token[]> {
        public Token[] apply(SeekIterator<LexerToken> p) {
            var loc = p.currentIndex();
            var r = left.apply(p);
            if (r == null) {
                p.setIndex(loc);
                r = right.apply(p);
                if (r == null) {
                    p.setIndex(loc);
                    return null;
                }
            }
            return r;
        }
    }
}
