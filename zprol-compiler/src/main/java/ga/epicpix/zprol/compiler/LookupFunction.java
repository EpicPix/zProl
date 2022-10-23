package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.compiler.precompiled.PreFunction;
import ga.epicpix.zprol.structures.Function;

public final class LookupFunction {
    public final boolean isClassMethod;
    public final PreFunction func;
    public final Function genFunc;
    public final String namespace;

    LookupFunction(boolean isClassMethod, PreFunction func, String namespace) {
        this.isClassMethod = isClassMethod;
        this.func = func;
        this.genFunc = null;
        this.namespace = namespace;
    }

    LookupFunction(boolean isClassMethod, Function func, String namespace) {
        this.isClassMethod = isClassMethod;
        this.func = null;
        this.genFunc = func;
        this.namespace = namespace;
    }

}
