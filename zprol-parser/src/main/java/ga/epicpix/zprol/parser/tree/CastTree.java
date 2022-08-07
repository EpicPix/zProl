package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;

public record CastTree(int start, int end, TypeTree type, boolean hardCast, IExpression value) implements IExpression {
    public CastTree(TypeTree type, boolean hardCast, IExpression value) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), type, hardCast, value);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}

