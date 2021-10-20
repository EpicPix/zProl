package ga.epicpix.zprol.tokens;

public class Token {

    private final TokenType type;

    public Token(TokenType type) {
        this.type = type;
    }

    public TokenType getType() {
        return type;
    }

    protected String getData() {
        return "";
    }

    public String toString() {
        String data = getData();
        if(data.isEmpty()) {
            return type.name().toLowerCase();
        }
        return type.name().toLowerCase() + "(" + data + ")";
    }
}
