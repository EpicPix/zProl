package ga.epicpix.zprol.precompiled;

import ga.epicpix.zprol.parser.tokens.Token;
import java.util.ArrayList;

public class PreFunction {

    public final ArrayList<PreFunctionModifiers> modifiers = new ArrayList<>();
    public String name;
    public String returnType;
    public final ArrayList<PreParameter> parameters = new ArrayList<>();
    public final ArrayList<Token> code = new ArrayList<>();

    public boolean hasCode() {
        for(PreFunctionModifiers modifier : modifiers) {
            if(modifier.isEmptyCode()) {
                return false;
            }
        }
        return true;
    }

}
