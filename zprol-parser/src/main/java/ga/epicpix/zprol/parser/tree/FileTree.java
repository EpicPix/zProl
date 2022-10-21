package ga.epicpix.zprol.parser.tree;

import java.util.List;

public final class FileTree implements ITree {
    private final int start;
    private final int end;
    public final List<IDeclaration> declarations;

    public FileTree(int start, int end, List<IDeclaration> declarations) {
        this.start = start;
        this.end = end;
        this.declarations = declarations;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    public NamespaceTree namespace() {
        if(!declarations.isEmpty() && declarations.get(0) instanceof NamespaceTree) {
            return (NamespaceTree) declarations.get(0);
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
