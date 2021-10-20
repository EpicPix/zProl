package ga.epicpix.zprol.tokens;

import ga.epicpix.zprol.ParameterDataType;
import ga.epicpix.zprol.ParserFlag;
import java.util.ArrayList;

public class FunctionToken extends Token {

    private final String returnType;
    private final ArrayList<ParameterDataType> parameters;
    private final String name;
    private final ArrayList<ParserFlag> flags;

    public FunctionToken(String returnType, String name, ArrayList<ParameterDataType> parameters, ArrayList<ParserFlag> flags) {
        super(TokenType.START_FUNCTION);
        this.returnType = returnType;
        this.name = name;
        this.parameters = parameters;
        this.flags = flags;
    }

    protected String getData() {
        return "name=\"" + name + "\", returnType=\"" + returnType + "\", parameters=" + parameters + ", flags=" + flags;
    }

}
