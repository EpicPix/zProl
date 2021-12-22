package ga.epicpix.zprol.precompiled;

import ga.epicpix.zprol.parser.tokens.Token;
import java.util.ArrayList;

public class PreFunction {

    public String name;
    public String returnType;
    public ArrayList<PreParameter> parameters = new ArrayList<>();
    public ArrayList<Token> code = new ArrayList<>();

}
