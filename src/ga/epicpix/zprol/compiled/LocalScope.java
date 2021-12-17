package ga.epicpix.zprol.compiled;

import ga.epicpix.zprol.exceptions.VariableAlreadyDefinedException;
import ga.epicpix.zprol.exceptions.VariableNotDefinedException;
import java.util.ArrayList;

public class LocalScope {

    public final LocalScope parent;

    private int localVariableSizeIndex = 0;
    private int min = 0;
    private ArrayList<LocalVariable> localVariables = new ArrayList<>();

    public LocalScope() {
        this.parent = null;
    }

    public LocalScope(LocalScope parent) {
        this.parent = parent;
    }

    public LocalVariable defineLocalVariable(String name, Type type) {
        if(findLocalVariable(name) != null) {
            throw new VariableAlreadyDefinedException(name);
        }
        localVariableSizeIndex += type.getSize();
        LocalVariable localVar = new LocalVariable(name, type, localVariableSizeIndex);
        localVariables.add(localVar);
        return localVar;
    }

    public LocalVariable getLocalVariable(String name) {
        for(LocalVariable lVar : localVariables) {
            if(lVar.name.equals(name)) {
                return lVar;
            }
        }
        if(parent != null) return parent.getLocalVariable(name);
        throw new VariableNotDefinedException(name);
    }

    public LocalVariable findLocalVariable(String name) {
        for(LocalVariable lVar : localVariables) {
            if(lVar.name.equals(name)) {
                return lVar;
            }
        }
        if(parent != null) return parent.findLocalVariable(name);
        return null;
    }

    public int getScopeLocalVariableScope() {
        return localVariableSizeIndex;
    }

    public int getLocalVariablesSize() {
        if(parent != null) return parent.getLocalVariablesSize() + localVariableSizeIndex + min;
        return localVariableSizeIndex + min;
    }

    public void addMin(int min) {
        if(this.min < min) {
            this.min = min;
        }
    }
}
