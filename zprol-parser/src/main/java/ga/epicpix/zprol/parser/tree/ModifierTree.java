package ga.epicpix.zprol.parser.tree;

public final class ModifierTree implements ITree {

    public static final int NATIVE = 1 << 0;

    private final int start;
    private final int end;
    public final long mod;

    public ModifierTree(int start, int end, long mod) {
        this.start = start;
        this.end = end;
        this.mod = mod;
    }

    public int getStartIndex() {
        return start;
    }

    public int getEndIndex() {
        return end;
    }
}
