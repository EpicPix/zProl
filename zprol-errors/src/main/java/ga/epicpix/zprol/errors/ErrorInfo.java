package ga.epicpix.zprol.errors;

public class ErrorInfo {

    public final ErrorCodes code;
    public final ErrorLocation location;
    public final String message;
    public final Object[] format;

    public ErrorInfo(ErrorCodes code, ErrorLocation location, String message, Object[] format) {
        this.code = code;
        this.location = location;
        this.message = message;
        this.format = format;
    }
}
