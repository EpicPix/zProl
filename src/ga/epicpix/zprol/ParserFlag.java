package ga.epicpix.zprol;

public enum ParserFlag {

    INTERNAL,
    STATIC,
    NO_IMPLEMENTATION(false),
    ;

    private boolean publicFlag;

    ParserFlag() {
        this(true);
    }

    ParserFlag(boolean publicFlag) {
        this.publicFlag = publicFlag;
    }

    public boolean isPublicFlag() {
        return publicFlag;
    }
}
