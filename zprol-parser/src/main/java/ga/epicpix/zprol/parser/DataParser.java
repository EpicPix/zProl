package ga.epicpix.zprol.parser;

public class DataParser {

    public static char[] genCharacters(char from, char to) {
        char[] out = new char[to-from+1];
        for(char i = from; i <= to; i++) out[i-from] = i;
        return out;
    }

    public static int[] genCharacters(int from, int to) {
        int[] out = new int[to-from+1];
        for(int i = from; i <= to; i++) out[i-from] = i;
        return out;
    }

    public static char[] joinCharacters(char[]... chars) {
        int requiredSpace = 0;
        for(char[] c : chars) requiredSpace += c.length;
        char[] c = new char[requiredSpace];
        int current = 0;
        for(char[] a : chars) for(char b : a) c[current++] = b;
        return c;
    }

    public static boolean matchesCharacters(char[] chars, int c) {
        if(chars == null) return false;
        for(int t : chars) if(t == c) return true;
        return false;
    }

    public static boolean matchesCharacters(int[] chars, int c) {
        if(chars == null) return false;
        for(int t : chars) if(t == c) return true;
        return false;
    }

    public static boolean matchesCharacters(char[] chars, String c) {
        if(chars == null) return false;
        for(int i = 0; i<c.length(); i++) if(!matchesCharacters(chars, c.codePointAt(i))) return false;
        return true;
    }

    public static final char[] nonSpecialCharacters = joinCharacters(genCharacters('a', 'z'), genCharacters('A', 'Z'), genCharacters('0', '9'), new char[] {'_'});

    private final String data;
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

    public void setLocation(ParserLocation loc) {
        lineNumber = loc.line();
        lineRow = loc.row();
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

    public int seekCharacter() {
        return data.codePointAt(index);
    }

    public int seekNextCharacter() {
        return data.codePointAt(index + 1);
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

    public int nextChar(int[] allowedCharacters) {
        lastLocation = getLocation();
        if(!hasNext()) return -1;
        if(!matchesCharacters(allowedCharacters, data.codePointAt(index))) {
            return -1;
        }
        lineRow++;
        int cp = data.codePointAt(index++);
        if(cp == '\n') {
            lineNumber++;
            lineRow = 0;
        }
        return cp;
    }

    public int nextCharNot(int[] disallowedCharacters) {
        lastLocation = getLocation();
        if(!hasNext()) return -1;
        if(matchesCharacters(disallowedCharacters, data.codePointAt(index))) {
            return -1;
        }
        lineRow++;
        int cp = data.codePointAt(index++);
        if(cp == '\n') {
            lineNumber++;
            lineRow = 0;
        }
        return cp;
    }

    public String nextTemplateWord(char[] allowedCharacters) {
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

    public String seekTemplatedWord(char[] allowedCharacters) {
        var loc = saveLocation();
        String str = nextTemplateWord(allowedCharacters);
        loadLocation(loc);
        return str;
    }

}
