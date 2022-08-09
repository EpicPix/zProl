package ga.epicpix.zprol.compiler.precompiled;

import ga.epicpix.zprol.parser.tree.IStatement;
import ga.epicpix.zprol.structures.FunctionModifiers;

import java.util.ArrayList;

public class PreFunction {

    public final ArrayList<FunctionModifiers> modifiers = new ArrayList<>();
    public String name;
    public String returnType;
    public final ArrayList<PreParameter> parameters = new ArrayList<>();
    public final ArrayList<IStatement> code = new ArrayList<>();

    public boolean hasCode() {
        for(FunctionModifiers modifier : modifiers) {
            if(modifier.isEmptyCode()) {
                return false;
            }
        }
        return true;
    }

}
