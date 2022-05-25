package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.LanguageToken;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Function;

import static ga.epicpix.zprol.parser.zld.ZldParser.DEFINITIONS;

class CallToken extends LanguageTokenFragment {

    private static final Token[] EMPTY_TOKENS = new Token[0];

    CallToken(String use) {
        super(new CallTokenTokenReader(use), "$" + use);
    }


    static class CallTokenTokenReader implements Function<DataParser, Token[]> {

        private final String use;
        private ArrayList<LanguageToken> definitions;

        CallTokenTokenReader(String use) {
            this.use = use;
        }

        public Token[] apply(DataParser p) {
            if(definitions == null) {
                definitions = DEFINITIONS.get(use);
            }
            var startLocation = p.getLocation();

            fLoop: for(LanguageToken def : definitions) {
                var loc = p.saveLocation();
                var iterTokens = new ArrayList<Token>();
                for (var frag : def.args()) {
                    var r = frag.apply(p);
                    if (r == null) {
                        p.loadLocation(loc);
                        continue fLoop;
                    }
                    Collections.addAll(iterTokens, r);
                }
                if(def.clean()) return EMPTY_TOKENS;
                if(def.inline() || (def.merge() && iterTokens.size() == 1)) return iterTokens.toArray(EMPTY_TOKENS);
                return new Token[]{new NamedToken(use, startLocation, p.getLocation(), p, iterTokens.toArray(EMPTY_TOKENS))};
            }
            return null;
        }
    }
}
