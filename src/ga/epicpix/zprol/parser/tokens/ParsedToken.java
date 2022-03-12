package ga.epicpix.zprol.parser.tokens;

import java.util.ArrayList;

public class ParsedToken extends Token {

    public final String name;
    public final ArrayList<Token> tokens;

    public ParsedToken(String name, ArrayList<Token> tokens) {
        super(TokenType.PARSED);
        this.name = name;
        this.tokens = tokens;
    }

    protected String getData() {
        return name + " - " +  tokens;
    }
}
