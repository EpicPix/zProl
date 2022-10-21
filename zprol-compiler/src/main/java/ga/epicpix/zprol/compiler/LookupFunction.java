package ga.epicpix.zprol.compiler;

import ga.epicpix.zprol.compiler.precompiled.PreFunction;

public final class LookupFunction {
    public final boolean isClassMethod;
    public final PreFunction func;
    public final String namespace;

    LookupFunction(boolean isClassMethod, PreFunction func, String namespace) {
        this.isClassMethod = isClassMethod;
        this.func = func;
        this.namespace = namespace;
    }

}
