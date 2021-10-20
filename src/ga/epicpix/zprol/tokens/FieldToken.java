package ga.epicpix.zprol.tokens;

public class FieldToken extends Token {

    private final String type;
    private final String name;

    public FieldToken(String type, String name) {
        super(TokenType.FIELD);
        this.type = type;
        this.name = name;
    }

    protected String getData() {
        return "name=\"" + name + "\", type=\"" + type + "\"";
    }

}
