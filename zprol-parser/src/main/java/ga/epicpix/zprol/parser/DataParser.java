package ga.epicpix.zprol.parser;

public class DataParser {

    public final String data;
    private final String fileName;
    private final String[] lines;
    private int index;

    private int lineNumber;
    private int lineRow;

    private ParserLocation lastLocation = new ParserLocation(0, 0);

    public DataParser(String fileName, String... lines) {
        this.fileName = fileName;
        this.lines = lines;
        data = String.join("\n", lines);
    }

    public int getIndex() {
        return index;
    }

    public ParserLocation getLocation(int index) {
        if(index < 0 || index > data.length()) throw new IllegalArgumentException("Index out of range");
        var line = 0;
        var len = 0;
        while(len <= index) {
            if(line == lines.length) {
                line = lines.length - 1;
                break;
            }else {
                if (lines[line].length() + len + line >= index) {
                    break;
                }
            }
            len += lines[line].length();
            line++;
        }
        return new ParserLocation(line, index - len - line);
    }

    public static class SavedLocation {
        private int savedStart;
        private int savedCurrentLine;
        private int savedCurrentLineRow;
        private ParserLocation savedLast;
    }

    public SavedLocation saveLocation() {
        SavedLocation savedLocation = new SavedLocation();
        savedLocation.savedStart = index;
        savedLocation.savedCurrentLine = lineNumber;
        savedLocation.savedCurrentLineRow = lineRow;
        savedLocation.savedLast = lastLocation;
        return savedLocation;
    }

    public void loadLocation(SavedLocation savedLocation) {
        index = savedLocation.savedStart;
        lineNumber = savedLocation.savedCurrentLine;
        lineRow = savedLocation.savedCurrentLineRow;
        lastLocation = savedLocation.savedLast;
    }

    public String[] getLines() {
        return lines;
    }

    public ParserLocation getLocation() {
        return new ParserLocation(lineNumber, lineRow);
    }

    public ParserLocation getLastLocation() {
        return lastLocation;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean hasNext() {
        return index < data.length();
    }

    public void goBack() {
        if(index <= 0) throw new IllegalStateException("Index is 0, cannot go further back");
        index--;
        lineRow--;
        if(lineRow != -1) return;
        lineNumber--;
        lineRow = lines[lineNumber].length() - 1;
        lastLocation = getLocation();
    }

    public int nextChar() {
        lastLocation = getLocation();
        if(!hasNext()) return -1;
        lineRow++;
        int cp = data.codePointAt(index++);
        if(cp == '\n') {
            lineNumber++;
            lineRow = 0;
        }
        return cp;
    }

}
