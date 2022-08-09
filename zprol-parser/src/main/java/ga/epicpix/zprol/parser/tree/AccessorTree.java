package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;

public record AccessorTree(int start, int end, IAccessorElement[] accessorElements) implements IExpression {
    public AccessorTree(IAccessorElement[] accessorElements) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), accessorElements);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
