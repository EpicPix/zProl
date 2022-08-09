package ga.epicpix.zprol.parser.tree;

import ga.epicpix.zprol.parser.ParserState;
import ga.epicpix.zprol.parser.tokens.LexerToken;

public record IfStatementTree(int start, int end, IExpression expression, CodeTree code, ElseStatementTree elseStatement) implements IStatement {
    public IfStatementTree(IExpression expression, CodeTree code, ElseStatementTree elseStatement) {
        this(ParserState.popStartLocation(), ParserState.getEndLocation(), expression, code, elseStatement);
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
