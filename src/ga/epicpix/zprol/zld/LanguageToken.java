package ga.epicpix.zprol.zld;

public record LanguageToken(String name, boolean inline, boolean saveable, boolean keyword, LanguageTokenFragment... args) {}
