package ga.epicpix.zprol.tokens;

import ga.epicpix.zprol.ParameterDataType;
import java.util.ArrayList;

public class FunctionToken extends Token {

    private final String returnType;
    private final ArrayList<ParameterDataType> parameters;
    private final String name;

    public FunctionToken(String returnType, String name, ArrayList<ParameterDataType> parameters) {
        super(TokenType.FUNCTION);
        this.returnType = returnType;
        this.name = name;
        this.parameters = parameters;
    }

    protected String getData() {
        return "name=\"" + name + "\", returnType=\"" + returnType + "\", parameters=" + parameters;
    }

}
