package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;

import java.util.List;

public record ParametersTree(int start, int end, ParameterTree[] parameters) implements ITree {
    public ParametersTree(ParameterTree[] parameters) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), parameters);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
