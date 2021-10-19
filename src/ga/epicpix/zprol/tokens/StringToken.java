package ga.epicpix.zprol.tokens;

public class StringToken extends Token {

    private final String string;

    public StringToken(String string) {
        super(TokenType.STRING);
        this.string = string;
    }

    public String getString() {
        return string;
    }

    protected String getData() {
        return super.getData() + ", string=\"" + string.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
