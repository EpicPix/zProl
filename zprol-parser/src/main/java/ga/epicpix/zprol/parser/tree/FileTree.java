package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;

import java.util.List;

public record FileTree(int start, int end, List<IDeclaration> declarations) implements ITree {
    public FileTree(List<IDeclaration> declarations) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), declarations);
    }

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
