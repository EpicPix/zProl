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

class MultiToken extends LanguageTokenFragment {

    MultiToken(LanguageTokenFragment fragment) {
        super(new MultiTokenTokenReader(fragment), fragment.getDebugName() + "*");
    }

    public record MultiTokenTokenReader(LanguageTokenFragment fragment) implements Function<SeekIterator<LexerToken>, Token[]> {
        public Token[] apply(SeekIterator<LexerToken> p) {
            ArrayList<Token> tokens = new ArrayList<>();
            boolean successful = false;

            do {
                var loc = p.currentIndex();
                ArrayList<Token> iterTokens = new ArrayList<>();
                var r = fragment.apply(p);
                if (r == null) {
                    p.setIndex(loc);
                    if (successful) {
                        break;
                    } else {
                        return EMPTY_TOKENS;
                    }
                }
                Collections.addAll(iterTokens, r);
                successful = true;
                tokens.addAll(iterTokens);
            } while (true);
            return tokens.toArray(EMPTY_TOKENS);
        }
    }

}
