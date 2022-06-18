package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.compiler.compiled.locals.LocalScopeManager;
import ga.epicpix.zprol.compiler.precompiled.PreClass;

import java.util.ArrayList;

public class FunctionCodeScope {

    public enum ScopeType {
        MAIN, IF, ELSE, WHILE
    }

    public final ScopeType scopeType;
    public final FunctionCodeScope previous;
    public final LocalScopeManager localsManager;
    public final PreClass thisClass;

    public final ArrayList<Integer> breakLocations;
    public final ArrayList<Integer> continueLocations;

    public FunctionCodeScope(LocalScopeManager localsManager, PreClass thisClass) {
        scopeType = ScopeType.MAIN;
        previous = null;
        this.localsManager = localsManager;
        this.thisClass = thisClass;
        breakLocations = continueLocations = null;
    }

    public FunctionCodeScope(ScopeType scopeType, FunctionCodeScope previous) {
        this.scopeType = scopeType;
        this.previous = previous;
        localsManager = previous.localsManager;
        thisClass = previous.thisClass;
        if(scopeType == ScopeType.WHILE) {
            breakLocations = new ArrayList<>();
            continueLocations = new ArrayList<>();
        }else {
            breakLocations = continueLocations = null;
        }
    }

    public void start() {
        localsManager.newScope();
    }

    public void finish() {
        localsManager.leaveScope();
    }

    public void addBreakLocation(int location) {
        if(scopeType == ScopeType.WHILE) {
            breakLocations.add(location);
        }else {
            throw new IllegalStateException("Cannot add break location when the scope type is not while");
        }
    }

    public void addContinueLocation(int location) {
        if(scopeType == ScopeType.WHILE) {
            continueLocations.add(location);
        }else {
            throw new IllegalStateException("Cannot add continue location when the scope type is not while");
        }
    }

}
