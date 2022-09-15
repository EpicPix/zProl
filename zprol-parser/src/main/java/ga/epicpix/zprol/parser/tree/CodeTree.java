package ga.epicpix.zprol.parser.tree;

import java.util.List;

public record CodeTree(int start, int end, List<IStatement> statements) implements ITree {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
