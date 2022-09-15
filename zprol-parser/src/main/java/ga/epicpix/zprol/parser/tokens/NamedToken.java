package ga.epicpix.zprol.parser.tokens;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class NamedToken extends Token {

    public final Token[] tokens;

    public NamedToken(TokenType type, Token... tokens) {
        super(type, tokens[0].parser);
        this.tokens = tokens;
    }

    public NamedToken(TokenType type, List<Token> tokens) {
        this(type, tokens.toArray(new Token[0]));
    }

    public ArrayList<NamedToken> getTokensWithName(TokenType type) {
        ArrayList<NamedToken> t = new ArrayList<>();
        for(Token token : tokens) {
            if(token instanceof NamedToken named) {
                if(named.type == type) {
                    t.add(named);
                }
            }
        }
        return t;
    }

    public NamedToken getTokenWithName(TokenType type) {
        for(Token token : tokens) {
            if(token instanceof NamedToken named) {
                if(named.type == type) {
                    return named;
                }
            }
        }
        return null;
    }

    public LexerToken getLexerToken(TokenType type) {
        for(Token token : tokens) {
            if(token instanceof LexerToken lex) {
                if(lex.type == type) {
                    return lex;
                }
            }
        }
        return null;
    }

    public String getTokenAsString(TokenType type) {
        for(Token token : tokens) {
            if(token instanceof NamedToken named) {
                if(named.type == type) {
                    return Arrays.stream(named.tokens).map(Token::toStringRaw).collect(Collectors.joining());
                }
            }else if(token instanceof LexerToken lexer) {
                if(lexer.type == type) {
                    return lexer.toStringRaw();
                }
            }
        }
        return null;
    }

    protected String getData() {
        return type + Arrays.toString(tokens);
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
