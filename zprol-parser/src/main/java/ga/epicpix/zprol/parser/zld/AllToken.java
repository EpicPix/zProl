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

class AllToken extends LanguageTokenFragment {

    AllToken(LanguageTokenFragment[] fragments) {
        super(new AllTokenTokenReader(fragments), "(" + Arrays.stream(fragments).map(LanguageTokenFragment::getDebugName).collect(Collectors.joining(" ")) + ")");
    }

    public record AllTokenTokenReader(LanguageTokenFragment[] fragments) implements Function<SeekIterator<LexerToken>, Token[]> {
        public Token[] apply(SeekIterator<LexerToken> p) {
            var loc = p.currentIndex();
            ArrayList<Token> iterTokens = new ArrayList<>();
            for (var frag : fragments) {
                var r = frag.apply(p);
                if (r == null) {
                    p.setIndex(loc);
                    return null;
                }
                Collections.addAll(iterTokens, r);
            }
            return iterTokens.toArray(new Token[0]);
        }
    }

}
