package ga.epicpix.zprol.compiled.locals;

import ga.epicpix.zprol.compiled.PrimitiveType;
import ga.epicpix.zprol.exceptions.compilation.VariableAlreadyDefinedException;
import ga.epicpix.zprol.exceptions.compilation.VariableNotDefinedException;
import ga.epicpix.zprol.compiled.Type;

import java.util.ArrayList;

public class LocalScope {

    public final LocalScope parent;

    private final ArrayList<LocalVariable> localVariables = new ArrayList<>();
    private int localVariableSizeIndex = 0;
    private int used = 0;

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
        if(type instanceof PrimitiveType primitive) {
            localVariableSizeIndex += primitive.getSize();
        }else {
            localVariableSizeIndex += 8;
        }
        LocalVariable localVar = new LocalVariable(name, type, localVariableSizeIndex);
        localVariables.add(localVar);
        return localVar;
    }

    public LocalVariable getLocalVariable(String name) {
        for(LocalVariable lVar : localVariables) {
            if(lVar.name().equals(name)) {
                return lVar;
            }
        }
        if(parent != null) return parent.getLocalVariable(name);
        throw new VariableNotDefinedException(name);
    }

    public LocalVariable tryGetLocalVariable(String name) {
        for(LocalVariable lVar : localVariables) {
            if(lVar.name().equals(name)) {
                return lVar;
            }
        }
        if(parent != null) return parent.tryGetLocalVariable(name);
        return null;
    }

    public LocalVariable findLocalVariable(String name) {
        for(LocalVariable lVar : localVariables) {
            if(lVar.name().equals(name)) {
                return lVar;
            }
        }
        if(parent != null) return parent.findLocalVariable(name);
        return null;
    }

    public int getLocalUsed() {
        return localVariableSizeIndex;
    }

    public int getLocalVariablesSize() {
        if(parent != null) return parent.getLocalVariablesSize() + localVariableSizeIndex + used;
        return localVariableSizeIndex + used;
    }

    public void setParentUsed(int used) {
        if(this.used < used) {
            this.used = used;
        }
    }
}
