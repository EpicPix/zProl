package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.utils.SeekIterator;

import java.util.function.Function;

import static ga.epicpix.zprol.parser.zld.CallToken.EMPTY_TOKENS;

class IgnoredToken extends LanguageTokenFragment {

    IgnoredToken(LanguageTokenFragment fragment) {
        super(new IgnoredTokenTokenReader(fragment), fragment.getDebugName() + "&");
    }

    public record IgnoredTokenTokenReader(LanguageTokenFragment fragment) implements Function<SeekIterator<LexerToken>, Token[]> {
        public Token[] apply(SeekIterator<LexerToken> p) {
            var loc = p.currentIndex();
            if (fragment.apply(p) == null) p.setIndex(loc);
            return EMPTY_TOKENS;
        }
    }
}
