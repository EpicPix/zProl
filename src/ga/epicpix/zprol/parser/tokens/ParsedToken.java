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

    public ArrayList<NamedToken> getTokensWithName(String name) {
        ArrayList<NamedToken> t = new ArrayList<>();
        for(Token token : tokens) {
            if(token instanceof NamedToken named) {
                if(named.name.equals(name)) {
                    t.add(named);
                }
            }
        }
        return t;
    }

    protected String getData() {
        return name + " - " +  tokens;
    }
}
