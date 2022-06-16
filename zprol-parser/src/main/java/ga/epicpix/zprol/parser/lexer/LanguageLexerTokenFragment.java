package ga.epicpix.zprol.parser.lexer;

public class LanguageLexerTokenFragment {

    private final int[] characters;
    private final boolean multi;
    private final boolean negate;

    public LanguageLexerTokenFragment(boolean multi, boolean negate, int... characters) {
        this.characters = characters;
        this.multi = multi;
        this.negate = negate;
    }

    public int[] getCharacters() {
        return characters;
    }

    public boolean isMulti() {
        return multi;
    }

    public boolean isNegate() {
        return negate;
    }

}
