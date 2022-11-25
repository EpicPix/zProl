package ga.epicpix.zprol.errors;

public class ErrorInfo {

    public final ErrorCodes code;
    public final String message;
    public final Object[] format;

    public ErrorInfo(ErrorCodes code, String message, Object[] format) {
        this.code = code;
        this.message = message;
        this.format = format;
    }
}
