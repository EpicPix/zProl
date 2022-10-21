package ga.epicpix.zprol.compiler.compiled.locals;

import ga.epicpix.zprol.types.Type;

public final class LocalVariable {
    public final String name;
    public final Type type;
    public final int index;

    LocalVariable(String name, Type type, int index) {
        this.name = name;
        this.type = type;
        this.index = index;
    }

}
