package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;
import ga.epicpix.zprol.parser.tokens.LexerToken;

public record CreateAssignmentStatementTree(int start, int end, TypeTree type, LexerToken name, IExpression expression) implements IStatement {
    public CreateAssignmentStatementTree(TypeTree type, LexerToken name, IExpression expression) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), type, name, expression);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
