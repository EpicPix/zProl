package ga.epicpix.zprol.errors;

public enum LineMode {

    NONE(""),

    LINE_HIGHLIGHT(ErrorStrings.LINE_HIGHLIGHT),
    LINE_REPLACE(ErrorStrings.LINE_REPLACE),
    LINE_REPLACE_FULL(ErrorStrings.LINE_REPLACE_FULL),
    LINE_REPLACE_UNKNOWN(ErrorStrings.LINE_REPLACE_UNKNOWN),
    LINE_START_UNKNOWN(ErrorStrings.LINE_START_UNKNOWN),
    LINE_END_UNKNOWN(ErrorStrings.LINE_END_UNKNOWN);

    public final String display;

    LineMode(String display) {
        this.display = display;
    }
}
