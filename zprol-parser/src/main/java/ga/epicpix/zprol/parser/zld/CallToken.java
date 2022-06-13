package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.LanguageToken;
import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.utils.SeekIterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Function;

import static ga.epicpix.zprol.parser.zld.ZldParser.DEFINITIONS;

class CallToken extends LanguageTokenFragment {

    static final Token[] EMPTY_TOKENS = new Token[0];

    CallToken(String use) {
        super(new CallTokenTokenReader(use), "$" + use);
    }


    public static class CallTokenTokenReader implements Function<SeekIterator<LexerToken>, Token[]> {

        public final String use;
        private ArrayList<LanguageToken> definitions;

        CallTokenTokenReader(String use) {
            this.use = use;
        }

        public Token[] apply(SeekIterator<LexerToken> tokens) {
            if(definitions == null) {
                definitions = DEFINITIONS.get(use);
            }
            if(definitions == null) {
                throw new NullPointerException("Definition '" + use + "' is not defined");
            }
            var startIndex = tokens.currentIndex();

            fLoop: for(LanguageToken def : definitions) {
                var loc = tokens.currentIndex();
                var iterTokens = new ArrayList<Token>();
                int rl = 0;
                for (var frag : def.args()) {
                    var r = frag.apply(tokens);
                    if (r == null) {
                        tokens.setIndex(loc);
                        continue fLoop;
                    }
                    rl += r.length;
                    if(!def.merge()) {
                        Collections.addAll(iterTokens, r);
                    }else {
                        for(Token t : r) {
                            if(t instanceof NamedToken nt && nt.name.equals(use)) {
                                Collections.addAll(iterTokens, nt.tokens);
                            }else {
                                iterTokens.add(t);
                            }
                        }
                    }
                }
                if(def.inline() || (def.merge() && rl == 1)) return iterTokens.toArray(EMPTY_TOKENS);
                return new Token[]{new NamedToken(use, tokens.get(startIndex).startLocation, tokens.current().endLocation, tokens.current().parser, iterTokens.toArray(EMPTY_TOKENS))};
            }
            return null;
        }
    }
}
