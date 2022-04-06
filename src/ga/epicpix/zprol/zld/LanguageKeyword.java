package ga.epicpix.zprol.zld;

public record LanguageKeyword(String keyword, String... tags) {

    public boolean hasTag(String tag) {
        for(String t : tags) {
            if(t.equals(tag)) {
                return true;
            }
        }
        return false;
    }

}
