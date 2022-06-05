package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.WordHolder;
import ga.epicpix.zprol.parser.tokens.WordToken;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ga.epicpix.zprol.parser.zld.CallToken.EMPTY_TOKENS;

class CharsWordToken extends LanguageTokenFragment {

    CharsWordToken(LanguageTokenFragment[] tokens) {
        super(new CharsWordTokenTokenReader(tokens), Arrays.stream(tokens).map(LanguageTokenFragment::getDebugName).collect(Collectors.joining(" ")));
    }

    public static class CharsWordTokenTokenReader implements Function<DataParser, Token[]> {

        public final LanguageTokenFragment[] tokens;

        public CharsWordTokenTokenReader(LanguageTokenFragment[] tokens) {
            this.tokens = tokens;
        }

        public Token[] apply(DataParser parser) {
            StringBuilder builder = new StringBuilder();
            var start = parser.getLocation();
            for(var c : tokens) {
                var loc = parser.saveLocation();
                var value = c.apply(parser);
                if(value == null) {
                    parser.loadLocation(loc);
                    return null;
                }
                for(var s : value) {
                    if(s instanceof WordHolder holder) {
                        builder.append(holder.getWord());
                    }else if(s instanceof NamedToken named) {
                        for(var t : named.tokens) {
                            if(t instanceof WordHolder holder) {
                                builder.append(holder.getWord());
                            }
                        }
                    }
                }
            }
            return new Token[] {new WordToken(builder.toString(), start, parser.getLocation(), parser)};
        }

    }

}
