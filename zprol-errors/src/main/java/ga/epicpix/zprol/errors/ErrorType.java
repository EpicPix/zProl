package ga.epicpix.zprol.errors;

public enum ErrorType {

    INFO("[INFO]"),
    WARN("[WARN]"),
    ERROR("[ERROR]"),
    CRITICAL("[CRITICAL]");

    public final String prefix;

    ErrorType(String prefix) {
        this.prefix = prefix;
    }
}
