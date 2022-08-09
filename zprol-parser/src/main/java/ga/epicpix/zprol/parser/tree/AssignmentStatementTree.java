package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;
import ga.epicpix.zprol.parser.tokens.LexerToken;

public record AssignmentStatementTree(int start, int end, AccessorTree accessor, IExpression expression) implements IStatement {
    public AssignmentStatementTree(AccessorTree accessor, IExpression expression) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), accessor, expression);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
