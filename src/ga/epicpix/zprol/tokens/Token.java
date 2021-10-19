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
        return type.name().toLowerCase();
    }

    public String toString() {
        return "Token(" + getData() + ")";
    }
}
