package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.lexer.LanguageLexerTokenFragment;

import java.util.function.Function;

class CharsToken extends LanguageLexerTokenFragment {

    private static String getDebugName(int[] characters) {
        StringBuilder debug = new StringBuilder();
        for (int c : characters) {
            debug.appendCodePoint(c);
        }
        return debug.toString();
    }

    CharsToken(boolean negate, int[] characters) {
        super(new CharsTokenTokenReader(negate, characters), "<" + (negate ? "^" : "") + getDebugName(characters) + ">");
    }

    public record CharsTokenTokenReader(boolean negate, int[] characters) implements Function<DataParser, String> {
        public String apply(DataParser p) {
            var loc = p.saveLocation();
            var res = negate ? p.nextCharNot(characters) : p.nextChar(characters);
            if (res == -1) {
                p.loadLocation(loc);
                return null;
            }
            return Character.toString(res);
        }
    }

}
