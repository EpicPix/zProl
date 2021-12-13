package ga.epicpix.zprol.tokens;

import java.util.ArrayList;

public class ParsedToken extends Token {

    public String name;
    public ArrayList<Token> tokens;

    public ParsedToken(String name, ArrayList<Token> tokens) {
        super(TokenType.PARSED);
        this.name = name;
        this.tokens = tokens;
    }

    protected String getData() {
        return name + " - " +  tokens;
    }
}
