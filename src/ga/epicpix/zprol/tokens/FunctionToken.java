package ga.epicpix.zprol.tokens;

import ga.epicpix.zprol.ParameterDataType;
import ga.epicpix.zprol.ParserFlag;
import java.util.ArrayList;

public class FunctionToken extends Token {

    public final String returnType;
    public final ArrayList<ParameterDataType> parameters;
    public final String name;
    public final ArrayList<ParserFlag> flags;

    public FunctionToken(String returnType, String name, ArrayList<ParameterDataType> parameters, ArrayList<ParserFlag> flags) {
        super(TokenType.FUNCTION);
        this.returnType = returnType;
        this.name = name;
        this.parameters = parameters;
        this.flags = flags;
    }

    protected String getData() {
        return "name=\"" + name + "\", returnType=\"" + returnType + "\", parameters=" + parameters + ", flags=" + flags;
    }

}
