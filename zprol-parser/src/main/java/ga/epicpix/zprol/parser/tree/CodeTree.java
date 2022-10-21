package ga.epicpix.zprol.parser.tree;

import java.util.List;

public final class CodeTree implements ITree {
    private final int start;
    private final int end;
    public final List<IStatement> statements;

    public CodeTree(int start, int end, List<IStatement> statements) {
        this.start = start;
        this.end = end;
        this.statements = statements;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}