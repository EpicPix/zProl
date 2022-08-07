package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;
import ga.epicpix.zprol.parser.tokens.LexerToken;

public record FieldTree(int start, int end, boolean isConst, TypeTree type, LexerToken name, IExpression value) implements IDeclaration {
    public FieldTree(boolean isConst, TypeTree type, LexerToken name, IExpression value) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), isConst, type, name, value);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
