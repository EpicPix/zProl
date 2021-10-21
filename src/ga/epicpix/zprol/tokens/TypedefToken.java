package ga.epicpix.zprol.tokens;

public class TypedefToken extends Token {

    private final String fromType;
    private final String toType;

    public TypedefToken(String fromType, String toType) {
        super(TokenType.TYPEDEF);
        this.fromType = fromType;
        this.toType = toType;
    }

    public String getName() {
        return fromType;
    }

    public String getToType() {
        return toType;
    }

    protected String getData() {
        return "\"" + fromType + "\", \"" + toType + "\"";
    }
}
