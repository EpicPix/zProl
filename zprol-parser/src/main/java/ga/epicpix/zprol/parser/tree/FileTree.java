package ga.epicpix.zprol.parser.tree;

import java.util.List;

public record FileTree(int start, int end, NamespaceTree namespace, List<IDeclaration> declarations) implements ITree {
    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
