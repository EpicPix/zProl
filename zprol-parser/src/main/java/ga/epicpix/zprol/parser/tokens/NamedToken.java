package ga.epicpix.zprol.parser.tokens;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.ParserLocation;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class NamedToken extends Token {

    public final Token[] tokens;

    public NamedToken(String name, ParserLocation startLocation, ParserLocation endLocation, DataParser parser, Token... tokens) {
        super(name, startLocation, endLocation, parser);
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

    public ArrayList<LexerToken> getLexerTokens(String name) {
        ArrayList<LexerToken> t = new ArrayList<>();
        for(Token token : tokens) {
            if(token instanceof LexerToken lex) {
                if(lex.name.equals(name)) {
                    t.add(lex);
                }
            }
        }
        return t;
    }

    public LexerToken getLexerToken(String name) {
        for(Token token : tokens) {
            if(token instanceof LexerToken lex) {
                if(lex.name.equals(name)) {
                    return lex;
                }
            }
        }
        return null;
    }

    public Token[] getNonWhitespaceTokens() {
        var tokens = new ArrayList<Token>();
        for(var token : this.tokens) {
            if(!(token instanceof LexerToken lexer && lexer.name.equals("Whitespace"))) {
                tokens.add(token);
            }
        }
        return tokens.toArray(new Token[0]);
    }

    public String getTokenAsString(String name) {
        for(Token token : tokens) {
            if(token instanceof NamedToken named) {
                if(named.name.equals(name)) {
                    return Arrays.stream(named.tokens).map(Token::toStringRaw).collect(Collectors.joining());
                }
            }else if(token instanceof LexerToken lexer) {
                if(lexer.name.equals(name)) {
                    return lexer.toStringRaw();
                }
            }
        }
        return null;
    }

    protected String getData() {
        return name + Arrays.toString(tokens);
    }

    public String toStringRaw() {
        StringBuilder out = new StringBuilder();
        for(var t : tokens) {
            out.append(t.toStringRaw());
        }
        return out.toString();
    }
}
