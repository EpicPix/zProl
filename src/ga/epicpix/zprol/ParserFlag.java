package ga.epicpix.zprol;

public enum ParserFlag {

    INTERNAL,
    NO_IMPLEMENTATION(false);

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
