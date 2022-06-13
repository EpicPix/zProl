package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.lexer.LanguageLexerTokenFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

class MultiLexerToken extends LanguageLexerTokenFragment {

    MultiLexerToken(LanguageLexerTokenFragment[] fragments) {
        super(new MultiTokenTokenReader(fragments), "{" + Arrays.stream(fragments).map(LanguageLexerTokenFragment::getDebugName).collect(Collectors.joining(" ")) + "}");
    }

    public record MultiTokenTokenReader(LanguageLexerTokenFragment[] fragments) implements Function<DataParser, String> {
        public String apply(DataParser p) {
            StringBuilder b = new StringBuilder();
            ArrayList<String> strs = new ArrayList<>();
            boolean successful = false;

            fLoop: do {
                var loc = p.saveLocation();
                for (var frag : fragments) {
                    var r = frag.apply(p);
                    if (r == null) {
                        p.loadLocation(loc);
                        if (successful) {
                            break fLoop;
                        } else {
                            return "";
                        }
                    }
                    Collections.addAll(strs, r);
                }
                successful = true;
                for(var t : strs) b.append(t);
                strs.clear();
            } while (true);
            return b.toString();
        }
    }

}
