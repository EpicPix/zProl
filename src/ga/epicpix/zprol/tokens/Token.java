package ga.epicpix.zprol.tokens;

public class Token {

    private final TokenType type;

    public Token(TokenType type) {
        this.type = type;
    }

    public TokenType getType() {
        return type;
    }
}
