package ga.epicpix.zprol.errors;

public class ErrorLocation {

    public final int startRow;
    public final int startLine;

    public final int endRow;
    public final int endLine;


    public final String filename;
    public final String[] lines;

    public ErrorLocation(int row, int line, String filename, String[] lines) {
        startRow = endRow = row;
        startLine = endLine = line;
        this.filename = filename;
        this.lines = lines;
    }

    public ErrorLocation(int startRow, int startLine, int endRow, int endLine, String filename, String[] lines) {
        this.startRow = startRow;
        this.startLine = startLine;
        this.endRow = endRow;
        this.endLine = endLine;
        this.filename = filename;
        this.lines = lines;
    }
}
