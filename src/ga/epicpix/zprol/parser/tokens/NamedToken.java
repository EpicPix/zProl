package ga.epicpix.zprol.parser.tokens;

import java.util.ArrayList;
import java.util.Arrays;

public class NamedToken extends Token {

    public final String name;
    public final Token[] tokens;

    public NamedToken(String name, Token... tokens) {
        super(TokenType.NAMED);
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

    public NamedToken getTokenWithName(String name) {
        for(Token token : tokens) {
            if(token instanceof NamedToken named) {
                if(named.name.equals(name)) {
                    return named;
                }
            }
        }
        return null;
    }

    protected String getData() {
        return name + Arrays.toString(tokens);
    }
}
