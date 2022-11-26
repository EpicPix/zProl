package ga.epicpix.zprol.errors;

class ErrorStrings {

    public static final String ANSI_RESET = "\u001b[0m";
    public static final String ANSI_RED = "\u001b[31m";
    public static final String ANSI_GREEN = "\u001b[32m";
    public static final String ANSI_YELLOW = "\u001b[33m";
    public static final String ANSI_CYAN = "\u001b[36m";

    public static final String LINE_HIGHLIGHT = ANSI_YELLOW + "~ %s" + ANSI_CYAN + "%s" + ANSI_YELLOW + "%s" + ANSI_RESET;
    public static final String LINE_REPLACE = ANSI_RED + "- %s\n" + ANSI_GREEN + "+ %s" + ANSI_CYAN + "%s" + ANSI_GREEN + "%s" + ANSI_RESET;
    public static final String LINE_REPLACE_FULL = ANSI_RED + "- %s\n" + ANSI_GREEN + "+ %s" + ANSI_RESET;
    public static final String LINE_REPLACE_UNKNOWN = ANSI_RED + "- %s\n" + ANSI_GREEN + "+ %s" + ANSI_CYAN + "???" + ANSI_GREEN + "%s" + ANSI_RESET;
    public static final String LINE_START_UNKNOWN = ANSI_RED + "- %s\n" + ANSI_GREEN + "+ %s" + ANSI_CYAN + " ???" + ANSI_GREEN + "%s" + ANSI_RESET;
    public static final String LINE_END_UNKNOWN = ANSI_RED + "- %s\n" + ANSI_GREEN + "+ %s" + ANSI_CYAN + "??? " + ANSI_GREEN + "%s" + ANSI_RESET;

}
