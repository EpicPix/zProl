package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.tokens.LexerToken;

public final class FunctionTree implements IDeclaration {
    private final int start;
    private final int end;
    public final ModifiersTree modifiers;
    public final TypeTree type;
    public final LexerToken name;
    public final ParametersTree parameters;
    public final CodeTree code;

    public FunctionTree(int start, int end, ModifiersTree modifiers, TypeTree type, LexerToken name, ParametersTree parameters, CodeTree code) {
        this.start = start;
        this.end = end;
        this.modifiers = modifiers;
        this.type = type;
        this.name = name;
        this.parameters = parameters;
        this.code = code;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
