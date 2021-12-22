package ga.epicpix.zprol.parser.tokens;

public class StringToken extends Token {

    private final String string;

    public StringToken(String string) {
        super(TokenType.STRING);
        this.string = string;
    }

    public String getString() {
        return string;
    }

    public String getData() {
        return "\"" + string.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
