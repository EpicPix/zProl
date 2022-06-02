package ga.epicpix.zprol.compiler.precompiled;

import ga.epicpix.zprol.structures.FunctionModifiers;

public enum PreFunctionModifiers {

    NATIVE("native", true, FunctionModifiers.NATIVE);

    public static final PreFunctionModifiers[] MODIFIERS = values();

    private final String name;
    private final boolean emptyCode;
    private final FunctionModifiers modifier;

    PreFunctionModifiers(String name, boolean emptyCode, FunctionModifiers modifier) {
        this.name = name;
        this.emptyCode = emptyCode;
        this.modifier = modifier;
    }

    public String getName() {
        return name;
    }

    public boolean isEmptyCode() {
        return emptyCode;
    }

    public FunctionModifiers getCompiledModifier() {
        return modifier;
    }

    public static PreFunctionModifiers getModifier(String name) {
        for(var modifier : MODIFIERS) {
            if(modifier.getName().equals(name)) {
                return modifier;
            }
        }
        throw new IllegalArgumentException("Unknown function modifier '" + name + "'");
    }

}
