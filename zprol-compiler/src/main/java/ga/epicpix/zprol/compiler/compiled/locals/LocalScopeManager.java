package ga.epicpix.zprol.compiler.compiled.locals;

import ga.epicpix.zprol.types.Type;

public class LocalScopeManager {

    private LocalScope currentScope = new LocalScope();

    public LocalVariable defineLocalVariable(String name, Type type) {
        return currentScope.defineLocalVariable(name, type);
    }

    public LocalVariable getLocalVariable(String name) {
        return currentScope.getLocalVariable(name);
    }

    public LocalVariable tryGetLocalVariable(String name) {
        return currentScope.tryGetLocalVariable(name);
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
        currentScope.parent.updateIndex(currentScope.getIndex());
        currentScope = currentScope.parent;
    }

}
