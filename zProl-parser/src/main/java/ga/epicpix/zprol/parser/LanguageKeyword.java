package ga.epicpix.zprol.parser;

import java.util.HashMap;

public record LanguageKeyword(String keyword, String... tags) {

    public static final HashMap<String, LanguageKeyword> KEYWORDS = new HashMap<>();

    public boolean hasTag(String tag) {
        for(String t : tags) {
            if(t.equals(tag)) {
                return true;
            }
        }
        return false;
    }

}
