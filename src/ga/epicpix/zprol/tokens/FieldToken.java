package ga.epicpix.zprol.tokens;

import ga.epicpix.zprol.ParserFlag;
import java.util.ArrayList;

public class FieldToken extends Token {

    public final String type;
    public final String name;
    public final ArrayList<ParserFlag> flags;

    public FieldToken(String type, String name, ArrayList<ParserFlag> flags) {
        super(TokenType.FIELD);
        this.type = type;
        this.name = name;
        this.flags = flags;
    }

    protected String getData() {
        return "name=\"" + name + "\", type=\"" + type + "\", flags=" + flags;
    }

}
