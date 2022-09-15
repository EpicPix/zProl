package ga.epicpix.zprol.parser.tree;

public record ModifierTree(int start, int end, long mod) implements ITree {

    public static final int NATIVE = 1 << 0;

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
