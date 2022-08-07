package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;

public record ArrayAccessTree(int start, int end, IExpression index) implements IAccessorElement {
    public ArrayAccessTree(IExpression index) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), index);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
