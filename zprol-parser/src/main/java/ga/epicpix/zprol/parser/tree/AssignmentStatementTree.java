package ga.epicpix.zprol.parser.tree;

import java.util.Objects;

public final class AssignmentStatementTree implements IStatement {
    private final int start;
    private final int end;
    public final AccessorTree accessor;
    public final IExpression expression;

    public AssignmentStatementTree(int start, int end, AccessorTree accessor, IExpression expression) {
        this.start = start;
        this.end = end;
        this.accessor = accessor;
        this.expression = expression;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }

}
