package ga.epicpix.zprol.compiler.precompiled;

import ga.epicpix.zprol.parser.tree.IExpression;

import java.util.ArrayList;

public class PreField {

    public PreField(IExpression defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String type;
    public String name;
    public final ArrayList<PreFieldModifiers> modifiers = new ArrayList<>();
    public final IExpression defaultValue;

    public boolean isConst() {
        for(PreFieldModifiers modifier : modifiers) {
            if(modifier.isConst()) {
                return true;
            }
        }
        return false;
    }

}
