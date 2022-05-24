package ga.epicpix.zprol.parser;

import java.util.HashMap;

public record LanguageKeyword(String keyword, String... tags) {

    public static final HashMap<String, LanguageKeyword> KEYWORDS = new HashMap<>();

    public static LanguageKeyword registerKeyword(String keyword, String... tags) {
        var lang = new LanguageKeyword(keyword, tags);
        KEYWORDS.put(keyword, lang);
        return lang;
    }

    public boolean hasTag(String tag) {
        for(String t : tags) {
            if(t.equals(tag)) {
                return true;
            }
        }
        return false;
    }

}
