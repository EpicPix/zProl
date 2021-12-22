package ga.epicpix.zprol.parser.tokens;

import java.util.ArrayList;

public class EquationToken extends Token {

    public final ArrayList<Token> tokens;

    public EquationToken(ArrayList<Token> tokens) {
        super(TokenType.EQUATION);
        this.tokens = tokens;
    }

    protected String getData() {
        return tokens.toString();
    }
}
