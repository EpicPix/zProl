package ga.epicpix.zprol.parser;

public final class ParserLocation {
    public final int line;
    public final int row;

    public ParserLocation(int line, int row) {
        this.line = line;
        this.row = row;
    }

    public String toString() {
        return "ParserLocation[line=" + line + ",row=" + row + "]";
    }
}
