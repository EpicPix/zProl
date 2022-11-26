package ga.epicpix.zprol.errors;

import static ga.epicpix.zprol.errors.LineMode.*;

public enum ErrorCodes {

    STD_STANDARD_LIBRARY_NOT_FOUND("STD0000", "Standard Library not found", NONE, ErrorType.WARN),

    LEX_UNEXPECTED_STRING_END     ("LEX0000", "Unexpected string ending fix:",            LINE_REPLACE, ErrorType.ERROR),
    LEX_INVALID_ESCAPE_SEQUENCE   ("LEX0001", "Invalid string escape sequence fix:", LINE_REPLACE_FULL, ErrorType.ERROR),
    LEX_INVALID_TOKEN             ("LEX0002", "Invalid token",                          LINE_HIGHLIGHT, ErrorType.ERROR),
    LEX_EXPECTED_NUMBER_AFTER_HEX ("LEX0003", "Expected hex after 0x",            LINE_REPLACE_UNKNOWN, ErrorType.ERROR),
    LEX_INVALID_HEX               ("LEX0004", "Invalid hex after 0x",                   LINE_HIGHLIGHT, ErrorType.ERROR),

    PARSE_EXPECTED_VALUE_GOT_EOF           ("PARSE0000", "Expected '%s' got end of file",                        NONE, ErrorType.CRITICAL),
    PARSE_EXPECTED_VALUE_GOT_OTHER_FIXABLE ("PARSE0001", "Expected '%s' got '%s', fix:",                 LINE_REPLACE, ErrorType.ERROR   ),
    PARSE_EXPECTED_VALUE_GOT_OTHER         ("PARSE0002", "Expected '%s' got '%s'",                     LINE_HIGHLIGHT, ErrorType.ERROR   ),
    PARSE_EXPECTED_VALUE_GOT_EOF_FIXABLE   ("PARSE0003", "Expected '%s' got end of file, fix:",          LINE_REPLACE, ErrorType.CRITICAL),
    PARSE_EXPECTED_EXPRESSION_GOT_OTHER    ("PARSE0004", "Expected an expression, but got '%s'", LINE_REPLACE_UNKNOWN, ErrorType.ERROR   ),
    PARSE_ARGS_NOT_VALID_PAREN_OR_SEMICOLON("PARSE0005", "Expected a ')' or ','",                        LINE_REPLACE, ErrorType.ERROR   ),



    ;

    private final String code, message;
    private final LineMode mode;
    private final ErrorType type;

    ErrorCodes(String code, String message, LineMode mode, ErrorType type) {
        this.code = code;
        this.message = message;
        this.mode = mode;
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public LineMode getMode() {
        return mode;
    }

    public String getMessage() {
        return message;
    }

    public ErrorType getType() {
        return type;
    }
}
