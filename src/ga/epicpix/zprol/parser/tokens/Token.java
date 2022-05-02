package ga.epicpix.zprol.parser.tokens;

import java.util.ArrayList;

public class Token {

    private final TokenType type;

    public Token(TokenType type) {
        this.type = type;
    }

    public WordToken asWordToken() {
        return (WordToken) this;
    }

    public KeywordToken asKeywordToken() {
        return (KeywordToken) this;
    }

    public NamedToken asNamedToken() {
        return (NamedToken) this;
    }

    public WordHolder asWordHolder() {
        return (WordHolder) this;
    }

    public OperatorToken asOperatorToken() {
        return (OperatorToken) this;
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
