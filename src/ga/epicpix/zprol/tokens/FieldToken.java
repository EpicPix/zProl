package ga.epicpix.zprol.tokens;

import ga.epicpix.zprol.ParserFlag;
import java.util.ArrayList;

public class FieldToken extends Token {

    public final String type;
    public final String name;
    public final ArrayList<Token> ops;
    public final ArrayList<ParserFlag> flags;

    public FieldToken(String type, String name, ArrayList<Token> ops, ArrayList<ParserFlag> flags) {
        super(TokenType.FIELD);
        this.type = type;
        this.name = name;
        this.ops = ops;
        this.flags = flags;
    }

    protected String getData() {
        return "name=\"" + name + "\", type=\"" + type + "\", ops=" + ops + ", flags=" + flags;
    }

}
