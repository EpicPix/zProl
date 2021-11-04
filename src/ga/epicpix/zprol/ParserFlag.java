package ga.epicpix.zprol;

public enum ParserFlag {

    INTERNAL,
    STATIC,
    FINAL,
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

    public static ParserFlag getFlag(String name) {
        for(ParserFlag flag : values()) {
            if(flag.isPublicFlag() && flag.name().toLowerCase().equals(name)) {
                return flag;
            }
        }
        return null;
    }
}
