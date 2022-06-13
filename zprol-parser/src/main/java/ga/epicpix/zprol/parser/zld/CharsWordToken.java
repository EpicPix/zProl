package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.lexer.LanguageLexerTokenFragment;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

class CharsWordToken extends LanguageLexerTokenFragment {

    CharsWordToken(boolean clean, LanguageLexerTokenFragment[] tokens) {
        super(new CharsWordTokenTokenReader(clean, tokens), Arrays.stream(tokens).map(LanguageLexerTokenFragment::getDebugName).collect(Collectors.joining(" ")));
    }

    public record CharsWordTokenTokenReader(boolean clean, LanguageLexerTokenFragment[] tokens) implements Function<DataParser, String> {
        public String apply(DataParser parser) {
            StringBuilder builder = new StringBuilder();
            for (var c : tokens) {
                var loc = parser.saveLocation();
                var value = c.apply(parser);
                if (value == null) {
                    parser.loadLocation(loc);
                    return null;
                }
                if (!clean) {
                    builder.append(value);
                }
            }
            return builder.toString();
        }

    }

}
