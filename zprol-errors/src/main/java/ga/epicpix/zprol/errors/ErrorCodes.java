package ga.epicpix.zprol.errors;

public enum ErrorCodes {

    STANDARD_LIBRARY_NOT_FOUND("STD0000", "Standard Library not found", ErrorType.WARN),

    EXPECTED_VALUE_GOT_EOF("PARSE0000", "Expected '%s' got end of file", ErrorType.CRITICAL),
    EXPECTED_VALUE_GOT_EOF_FIXABLE("PARSE0003", "Expected '%s' got end of file, fix:\n\u001b[31m- %s\n\u001b[32m+ %s\u001b[92m%s\u001b[32m%s\u001b[m", ErrorType.CRITICAL),
    EXPECTED_VALUE_GOT_OTHER_FIXABLE("PARSE0001", "Expected '%s' got '%s', fix:\n\u001b[31m- %s\n\u001b[32m+ %s\u001b[92m%s\u001b[32m%s\u001b[m", ErrorType.ERROR),
    EXPECTED_VALUE_GOT_OTHER("PARSE0002", "Expected '%s' got '%s'", ErrorType.ERROR),



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
