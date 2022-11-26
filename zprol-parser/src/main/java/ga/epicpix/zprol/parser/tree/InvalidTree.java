package ga.epicpix.zprol.parser.tree;

public class InvalidTree implements ITree {

    private final int start;
    private final int end;

    public InvalidTree(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
