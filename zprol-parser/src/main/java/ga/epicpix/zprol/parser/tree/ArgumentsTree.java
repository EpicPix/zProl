package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;

public record ArgumentsTree(int start, int end, IExpression[] arguments) implements ITree {
    public ArgumentsTree(IExpression[] arguments) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), arguments);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
