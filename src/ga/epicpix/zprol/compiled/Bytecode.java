package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.exceptions.VariableNotDefinedException;
import ga.epicpix.zprol.exceptions.VariableAlreadyDefinedException;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Bytecode {

    private ArrayList<LocalVariable> localVariables = new ArrayList<>();

    public LocalVariable defineLocalVariable(String name, Type type) {
        for(LocalVariable localVar : localVariables) {
            if(localVar.name.equals(name)) {
                throw new VariableAlreadyDefinedException(name);
            }
        }
        LocalVariable localVar = new LocalVariable(name, type, localVariables.size());
        localVariables.add(localVar);
        return localVar;
    }

    public LocalVariable getLocalVariable(String name) {
        for(LocalVariable lVar : localVariables) {
            if(lVar.name.equals(name)) {
                return lVar;
            }
        }
        throw new VariableNotDefinedException(name);
    }

    public void write(DataOutputStream out) throws IOException {
    }
}
