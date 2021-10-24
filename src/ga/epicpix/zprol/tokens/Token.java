package ga.epicpix.zprol.tokens;

import java.util.ArrayList;

public class Token {

    private final TokenType type;

    public Token(TokenType type) {
        this.type = type;
    }

    public static String toFriendlyString(ArrayList<Token> tokens) {
        StringBuilder builder = new StringBuilder();
        for(Token token : tokens) {
            if(token.getType() == TokenType.WORD) {
                builder.append(((WordToken) token).word);
            }else if(token.getType() == TokenType.ACCESSOR) {
                builder.append(".");
            }else if(token.getType() == TokenType.COMMA) {
                builder.append(", ");
            }else {
                builder.append("(").append(token.getType().name().toLowerCase()).append(")");
            }
        }
        return builder.toString();
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
