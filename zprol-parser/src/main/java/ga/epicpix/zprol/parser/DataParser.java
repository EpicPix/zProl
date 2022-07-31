package ga.epicpix.zprol.parser;

public class DataParser {

    public static int[] genCharacters(int from, int to) {
        int[] out = new int[to-from+1];
        for(int i = from; i <= to; i++) out[i-from] = i;
        return out;
    }

    public static int[] joinCharacters(int[]... chars) {
        int requiredSpace = 0;
        for(int[] c : chars) requiredSpace += c.length;
        int[] c = new int[requiredSpace];
        int current = 0;
        for(int[] a : chars) for(int b : a) c[current++] = b;
        return c;
    }

    public static boolean matchesCharacters(int[] chars, int c) {
        if(chars == null) return false;
        for(int t : chars) if(t == c) return true;
        return false;
    }

    public static boolean matchesCharacters(int[] chars, String c) {
        if(chars == null) return false;
        for(int i = 0; i<c.length(); i++) if(!matchesCharacters(chars, c.codePointAt(i))) return false;
        return true;
    }

    public static final int[] nonSpecialCharacters = joinCharacters(genCharacters('a', 'z'), genCharacters('A', 'Z'), genCharacters('0', '9'), new int[] {'_'});

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
                if (lines[line].length() + len >= index) {
                    break;
                }
            }
            len += lines[line].length();
            line++;
        }
        return new ParserLocation(line + 1, index - len + 1);
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

    public boolean checkNewLine() {
        if(!hasNext()) return false;
        return data.codePointAt(index) == '\n';
    }

    public void ignoreWhitespace() {
        while(index + 1 < data.length()) {
            if(!Character.isWhitespace(data.codePointAt(index)) && data.codePointAt(index) != '\n') {
                break;
            }
            lineRow++;
            if(data.codePointAt(index) == '\n') {
                lineNumber++;
                lineRow = 0;
            }
            index++;
        }
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

    public String nextTemplateWord(int[] allowedCharacters) {
        ignoreWhitespace();
        lastLocation = getLocation();
        if(!hasNext()) return null;
        StringBuilder word = new StringBuilder();
        while(hasNext() && !Character.isWhitespace(data.codePointAt(index))) {
            if(!matchesCharacters(allowedCharacters, data.codePointAt(index))) {
                if(word.length() == 0) {
                    lineRow++;
                    word.appendCodePoint(data.codePointAt(index++));
                }
                return word.toString();
            }else {
                if(!matchesCharacters(allowedCharacters, word.toString())) {
                    return word.toString();
                }
            }
            lineRow++;
            word.appendCodePoint(data.codePointAt(index++));
        }
        return word.toString();
    }

    public String nextWord() {
        return nextTemplateWord(nonSpecialCharacters);
    }

    public String seekWord() {
        var loc = saveLocation();
        String str = nextWord();
        loadLocation(loc);
        return str;
    }

}
