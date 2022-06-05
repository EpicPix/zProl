package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.WordToken;

import java.util.function.Function;

class CharsToken extends LanguageTokenFragment {

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

    public static class CharsTokenTokenReader implements Function<DataParser, Token[]> {

        public final boolean negate;
        public final int[] characters;

        CharsTokenTokenReader(boolean negate, int[] characters) {
            this.negate = negate;
            this.characters = characters;
        }

        public Token[] apply(DataParser p) {
            var startLocation = p.getLocation();
            var loc = p.saveLocation();
            var res = negate ? p.nextCharNot(characters) : p.nextChar(characters);
            if(res == -1) {
                p.loadLocation(loc);
                return null;
            }
            var endLocation = p.getLocation();
            return new Token[] {new WordToken(Character.toString(res), startLocation, endLocation, p)};
        }
    }

}
