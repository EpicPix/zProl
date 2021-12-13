package ga.epicpix.zprol.tokens;

import java.util.ArrayList;

public class Token {

    private final TokenType type;

    public Token(TokenType type) {
        this.type = type;
    }

    public static String toFriendlyString(ArrayList<Token> tokens) {
        StringBuilder builder = new StringBuilder();
        int indent = 0;
        int pendingIndent = 0;
        for(Token token : tokens) {
            if(token.getType() == TokenType.CLOSE_SCOPE) {
                indent--;
                pendingIndent = indent;
            }
            for(int i = 0; i<pendingIndent; i++) builder.append("    ");
            pendingIndent = 0;
            if(token.getType() == TokenType.WORD) {
                builder.append(((WordToken) token).word);
            }else if(token.getType() == TokenType.LONG_WORD) {
                builder.append(((LongWordToken) token).word);
            }else if(token.getType() == TokenType.OPERATOR) {
                builder.append(" ").append(((OperatorToken) token).operator).append(" ");
            }else if(token.getType() == TokenType.TYPE) {
                builder.append(((TypeToken) token).type().name).append(" ");
            }else if(token.getType() == TokenType.STRING) {
                builder.append("\"").append(((StringToken) token).getString().replace("\n", "\\n")).append("\"");
            }else if(token.getType() == TokenType.ACCESSOR) {
                builder.append(".");
            }else if(token.getType() == TokenType.COMMA) {
                builder.append(", ");
            }else if(token.getType() == TokenType.KEYWORD) {
                builder.append(((KeywordToken) token).keyword).append(" ");
            }else if(token.getType() == TokenType.END_LINE) {
                builder.append(";\n");
                pendingIndent = indent;
                continue;
            }else if(token.getType() == TokenType.OPEN) {
                builder.append("(");
            }else if(token.getType() == TokenType.CLOSE) {
                builder.append(")");
            }else if(token.getType() == TokenType.OPEN_SCOPE) {
                builder.append("{\n");
                indent++;
                pendingIndent = indent;
            }else if(token.getType() == TokenType.CLOSE_SCOPE) {
                builder.append("}\n");
            }else if(token.getType() == TokenType.NUMBER) {
                builder.append(((NumberToken) token).number);
            }else if(token.getType() == TokenType.EQUATION) {
                builder.append(toFriendlyString(((EquationToken) token).tokens));
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
