package ga.epicpix.zprol.parser.tokens;

import ga.epicpix.zprol.exceptions.CompileException;
import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.ParserLocation;

import java.util.ArrayList;
import java.util.Arrays;

public class NamedToken extends Token {

    public final String name;
    public final Token[] tokens;

    public NamedToken(String name, ParserLocation startLocation, ParserLocation endLocation, DataParser parser, Token... tokens) {
        super(TokenType.NAMED, startLocation, endLocation, parser);
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

    public Token getSingleTokenWithName(String name) {
        for(Token token : tokens) {
            if(token instanceof NamedToken named) {
                if(named.name.equals(name)) {
                    if(named.tokens.length != 1) {
                        throw new CompileException("Expected one token but found multiple", this);
                    }
                    return named.tokens[0];
                }
            }
        }
        return null;
    }

    protected String getData() {
        return name + Arrays.toString(tokens);
    }
}
