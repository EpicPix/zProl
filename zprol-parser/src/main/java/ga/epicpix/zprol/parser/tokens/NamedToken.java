package ga.epicpix.zprol.parser.tokens;

import ga.epicpix.zprol.parser.ParserLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class NamedToken extends Token {

    public final Token[] tokens;

    public NamedToken(String name, Token... tokens) {
        super(name, tokens[0].parser);
        this.tokens = tokens;
    }

    public NamedToken(String name, List<Token> tokens) {
        this(name, tokens.toArray(new Token[0]));
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

    public int getStart() {
        return tokens[0].getStart();
    }

    public int getEnd() {
        return tokens[tokens.length - 1].getEnd();
    }
}
