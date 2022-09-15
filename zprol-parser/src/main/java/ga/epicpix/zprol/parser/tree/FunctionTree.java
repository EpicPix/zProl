package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.tokens.LexerToken;

public record FunctionTree(int start, int end, ModifiersTree modifiers, TypeTree type, LexerToken name, ParametersTree parameters, CodeTree code) implements IDeclaration {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
