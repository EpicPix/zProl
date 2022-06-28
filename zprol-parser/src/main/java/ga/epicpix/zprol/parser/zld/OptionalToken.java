package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.utils.SeekIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ga.epicpix.zprol.parser.zld.CallToken.EMPTY_TOKENS;

class OptionalToken extends LanguageTokenFragment {

    OptionalToken(LanguageTokenFragment fragment) {
        super(new OptionalTokenTokenReader(fragment), fragment.getDebugName() + "?");
    }

    public record OptionalTokenTokenReader(LanguageTokenFragment fragment) implements Function<SeekIterator<LexerToken>, Token[]> {
        public Token[] apply(SeekIterator<LexerToken> p) {
            var loc = p.currentIndex();
            var r = fragment.apply(p);
            if (r == null) {
                p.setIndex(loc);
                return EMPTY_TOKENS;
            }
            return r;
        }
    }
}
