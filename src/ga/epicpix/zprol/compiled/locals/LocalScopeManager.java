package ga.epicpix.zprol.compiled.locals;

import ga.epicpix.zprol.compiled.PrimitiveType;

public class LocalScopeManager {

    private LocalScope currentScope = new LocalScope();

    public LocalVariable defineLocalVariable(String name, PrimitiveType type) {
        return currentScope.defineLocalVariable(name, type);
    }

    public LocalVariable getLocalVariable(String name) {
        return currentScope.getLocalVariable(name);
    }

    public LocalVariable tryGetLocalVariable(String name) {
        return currentScope.tryGetLocalVariable(name);
    }

    public LocalVariable findLocalVariable(String name) {
        return currentScope.findLocalVariable(name);
    }

    public int getLocalVariablesSize() {
        return currentScope.getLocalVariablesSize();
    }

    public void newScope() {
        currentScope = new LocalScope(currentScope);
    }

    public void leaveScope() {
        if(currentScope.parent == null) {
            throw new IllegalArgumentException("Cannot leave scope, no scopes available!");
        }
        currentScope.parent.setParentUsed(currentScope.getLocalUsed());
        currentScope = currentScope.parent;
    }

    public LocalScope getCurrentScope() {
        return currentScope;
    }

}
