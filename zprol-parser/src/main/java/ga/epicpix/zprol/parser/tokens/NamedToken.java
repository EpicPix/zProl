package ga.epicpix.zprol.parser.tokens;

import java.util.Arrays;

public final class NamedToken extends Token {

    public final Token[] tokens;

    public NamedToken(TokenType type, Token... tokens) {
        super(type, tokens[0].parser);
        this.tokens = tokens;
    }

    protected String getData() {
        return type + Arrays.toString(tokens);
    }

    public String toStringRaw() {
        StringBuilder out = new StringBuilder();
        for(Token t : tokens) {
            out.append(t.toStringRaw());
        }
        return out.toString();
    }

    public int getStart() {
        return tokens[0].getStart();
    }

    public int getEnd() {
        return tokens[tokens.length - 1].getEnd();
    }
}
