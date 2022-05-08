package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.exceptions.compilation.VariableAlreadyDefinedException;
import ga.epicpix.zprol.exceptions.compilation.VariableNotDefinedException;

import java.util.ArrayList;

public class Scope {

    private final ArrayList<Variable> variables = new ArrayList<>();

    public Variable addVariable(String name, PrimitiveType type) {
        for(Variable var : variables) {
            if(var.name().equals(name)) {
                throw new VariableAlreadyDefinedException(name);
            }
        }
        Variable var = new Variable(name, type);
        variables.add(var);
        return var;
    }

    public Variable getVariable(String name) {
        for(Variable var : variables) {
            if(var.name().equals(name)) {
                return var;
            }
        }
        throw new VariableNotDefinedException(name);
    }

    public static Variable getVariable(ArrayList<Scope> scopes, String name) {
        for(Scope scope : scopes) {
            for(Variable var : scope.variables) {
                if(var.name().equals(name)) {
                    return var;
                }
            }
        }
        throw new VariableNotDefinedException(name);
    }

}
