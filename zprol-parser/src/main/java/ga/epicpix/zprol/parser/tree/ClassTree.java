package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;
import ga.epicpix.zprol.parser.tokens.LexerToken;

import java.util.List;

public record ClassTree(int start, int end, LexerToken name, List<IDeclaration> declarations) implements IDeclaration {

    public ClassTree(LexerToken name, List<IDeclaration> declarations) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), name, declarations);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
