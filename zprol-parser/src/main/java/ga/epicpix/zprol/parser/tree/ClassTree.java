package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.tokens.LexerToken;

import java.util.List;

public final class ClassTree implements IDeclaration {
    private final int start;
    private final int end;
    public final LexerToken name;
    public final List<IDeclaration> declarations;

    public ClassTree(int start, int end, LexerToken name, List<IDeclaration> declarations) {
        this.start = start;
        this.end = end;
        this.name = name;
        this.declarations = declarations;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
