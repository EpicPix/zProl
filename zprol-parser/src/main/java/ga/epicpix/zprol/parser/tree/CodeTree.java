package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;

import java.util.List;

public record CodeTree(int start, int end, List<IStatement> statements) implements ITree {
    public CodeTree(List<IStatement> statements) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), statements);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
