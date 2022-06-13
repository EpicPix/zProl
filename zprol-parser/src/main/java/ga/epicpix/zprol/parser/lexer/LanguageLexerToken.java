package ga.epicpix.zprol.parser.lexer;

import java.util.ArrayList;

public record LanguageLexerToken(String name, LanguageLexerTokenFragment... args) {

    public static final ArrayList<LanguageLexerToken> LEXER_TOKENS = new ArrayList<>();

}
