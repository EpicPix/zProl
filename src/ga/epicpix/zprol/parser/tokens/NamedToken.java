package ga.epicpix.zprol.parser.tokens;

import java.util.Arrays;

public class NamedToken extends Token {

    public final String name;
    public final Token[] tokens;

    public NamedToken(String name, Token... tokens) {
        super(TokenType.NAMED);
        this.name = name;
        this.tokens = tokens;
    }

    protected String getData() {
        return name + Arrays.toString(tokens);
    }
}
