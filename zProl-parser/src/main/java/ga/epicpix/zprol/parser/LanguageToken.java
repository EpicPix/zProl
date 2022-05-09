package ga.epicpix.zprol.parser;

import java.util.ArrayList;

public record LanguageToken(String name, boolean inline, boolean saveable, boolean keyword, boolean clean, LanguageTokenFragment... args) {

    public static final ArrayList<LanguageToken> TOKENS = new ArrayList<>();

}
