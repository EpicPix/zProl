package ga.epicpix.zprol.parser;

import ga.epicpix.zprol.parser.zld.LanguageTokenFragment;

import java.util.ArrayList;

public record LanguageToken(String name, boolean inline, boolean keyword, boolean clean, boolean flip, LanguageTokenFragment... args) {

    public static final ArrayList<LanguageToken> TOKENS = new ArrayList<>();

}
