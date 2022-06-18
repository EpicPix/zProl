package ga.epicpix.zprol.compiler.compiled.locals;

import ga.epicpix.zprol.compiler.exceptions.VariableAlreadyDefinedException;
import ga.epicpix.zprol.compiler.exceptions.VariableNotDefinedException;
import ga.epicpix.zprol.types.NullType;
import ga.epicpix.zprol.types.PrimitiveType;
import ga.epicpix.zprol.types.Type;

import java.util.ArrayList;

public class LocalScope {

    public final LocalScope parent;

    private final ArrayList<LocalVariable> localVariables = new ArrayList<>();
    private int index = 0;

    public LocalScope() {
        this.parent = null;
    }

    public LocalScope(LocalScope parent) {
        this.parent = parent;
        this.index = parent.index;
    }

    public LocalVariable defineLocalVariable(String name, Type type) {
        if(type instanceof NullType) throw new IllegalArgumentException("Cannot define local variable with null type");
        if(findLocalVariable(name) != null) {
            throw new VariableAlreadyDefinedException(name);
        }
        if(type instanceof PrimitiveType primitive) {
            index += primitive.getSize();
        }else {
            index += 8;
        }
        LocalVariable localVar = new LocalVariable(name, type, getLocalVariablesSize());
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

    public int getIndex() {
        return index;
    }

    public int getLocalVariablesSize() {
        return index;
    }

    public void updateIndex(int index) {
        if(this.index < index) {
            this.index = index;
        }
    }
}
