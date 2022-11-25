package ga.epicpix.zprol.errors;

public enum ErrorCodes {

    STD_STANDARD_LIBRARY_NOT_FOUND("STD0000", "Standard Library not found", ErrorType.WARN),

    LEX_UNEXPECTED_STRING_END("LEX0000", "Unexpected string ending fix:\n\u001b[31m- %s\n\u001b[32m+ %s\u001b[36m%s\u001b[32m%s\u001b[m", ErrorType.ERROR),
    LEX_INVALID_ESCAPE_SEQUENCE("LEX0001", "Invalid string escape sequence fix:\n\u001b[31m- %s\n\u001b[32m+ %s\u001b[m", ErrorType.ERROR),
    LEX_INVALID_TOKEN("LEX0002", "Invalid token\n\u001b[33m~ %s\u001b[36m%s\u001b[33m%s\u001b[m", ErrorType.ERROR),
    LEX_EXPECTED_NUMBER_AFTER_HEX("LEX0003", "Expected hex after 0x\n\u001b[31m- %s\n\u001b[32m+ %s\u001b[36m???\u001b[32m%s\u001b[m", ErrorType.ERROR),
    LEX_INVALID_HEX("LEX0004", "Invalid hex after 0x\n\u001b[33m~ %s\u001b[36m%s\u001b[33m%s\u001b[m", ErrorType.ERROR),

    PARSE_EXPECTED_VALUE_GOT_EOF("PARSE0000", "Expected '%s' got end of file", ErrorType.CRITICAL),
    PARSE_EXPECTED_VALUE_GOT_OTHER_FIXABLE("PARSE0001", "Expected '%s' got '%s', fix:\n\u001b[31m- %s\n\u001b[32m+ %s\u001b[36m%s\u001b[32m%s\u001b[m", ErrorType.ERROR),
    PARSE_EXPECTED_VALUE_GOT_OTHER("PARSE0002", "Expected '%s' got '%s'\n\u001b[33m~ %s\u001b[36m%s\u001b[33m%s\u001b[m", ErrorType.ERROR),
    PARSE_EXPECTED_VALUE_GOT_EOF_FIXABLE("PARSE0003", "Expected '%s' got end of file, fix:\n\u001b[31m- %s\n\u001b[32m+ %s\u001b[36m%s\u001b[32m%s\u001b[m", ErrorType.CRITICAL),
    PARSE_EXPECTED_EXPRESSION_GOT_OTHER("PARSE0004", "Expected an expression, but got '%s'\n\u001b[31m- %s\n\u001b[32m+ %s\u001b[36m???\u001b[32m%s\u001b[m", ErrorType.ERROR),



    ;

    private final String code, message;
    private final ErrorType type;

    ErrorCodes(String code, String message, ErrorType type) {
        this.code = code;
        this.message = message;
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public ErrorType getType() {
        return type;
    }
}
