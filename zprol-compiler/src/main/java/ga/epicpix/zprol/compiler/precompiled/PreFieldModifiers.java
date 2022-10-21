package ga.epicpix.zprol.compiler.precompiled;

import ga.epicpix.zprol.structures.FieldModifiers;

public enum PreFieldModifiers {

    CONST("const", true, FieldModifiers.CONST);

    public static final PreFieldModifiers[] MODIFIERS = values();

    private final String name;
    private final boolean isConst;
    private final FieldModifiers modifier;

    PreFieldModifiers(String name, boolean isConst, FieldModifiers modifier) {
        this.name = name;
        this.isConst = isConst;
        this.modifier = modifier;
    }

    public String getName() {
        return name;
    }

    public boolean isConst() {
        return isConst;
    }

    public FieldModifiers getCompiledModifier() {
        return modifier;
    }

    public static PreFieldModifiers getModifier(String name) {
        for(PreFieldModifiers modifier : MODIFIERS) {
            if(modifier.getName().equals(name)) {
                return modifier;
            }
        }
        throw new IllegalArgumentException("Unknown field modifier '" + name + "'");
    }

}
