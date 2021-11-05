package ga.epicpix.zprol;

public class ParserLocation {

    public final int line;
    public final int row;

    public ParserLocation(int line, int row) {
        this.line = line;
        this.row = row;
    }

    public String toString() {
        return "ParserLocation(" + line + ", " + row + ")";
    }
}
