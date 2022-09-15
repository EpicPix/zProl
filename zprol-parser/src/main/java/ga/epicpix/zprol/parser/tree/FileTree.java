package ga.epicpix.zprol.parser.tree;

import java.util.List;

public record FileTree(int start, int end, List<IDeclaration> declarations) implements ITree {
    public NamespaceTree namespace() {
        if(!declarations.isEmpty() && declarations.get(0) instanceof NamespaceTree n) {
            return n;
        }
        return null;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
