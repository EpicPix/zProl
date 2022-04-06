package ga.epicpix.zprol.parser;

import java.util.Stack;

public class DataParser {

    public static char[] genCharacters(char from, char to) {
        char[] out = new char[to-from+1];
        for(char i = from; i <= to; i++) out[i-from] = i;
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

    public static boolean matchesCharacters(char[] chars, String c) {
        if(chars == null) return false;
        for(int i = 0; i<c.length(); i++) if(!matchesCharacters(chars, c.codePointAt(i))) return false;
        return true;
    }

    public static final char[] nonSpecialCharacters = joinCharacters(genCharacters('a', 'z'), genCharacters('A', 'Z'), genCharacters('0', '9'), new char[] {'_'});
    public static final char[] validDotWordCharacters = joinCharacters(nonSpecialCharacters, new char[] {'.'});
    public static final char[] validLongWordCharacters = joinCharacters(validDotWordCharacters, new char[] {'+', '-', '@', '!'});

    public static final char[] operatorCharacters = new char[] {'+', '=', '/', '*', '-', '%', '<', '>', '!', '&'};
    public static final char[] typeCharacters = joinCharacters(nonSpecialCharacters, new char[] {'.', '(', ')', '<', '>'});

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

    private final Stack<SavedLocation> locationStack = new Stack<>();

    public SavedLocation getSaveLocation() {
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

    public void saveLocation() {
        locationStack.push(getSaveLocation());
    }

    public void discardLocation() {
        locationStack.pop();
    }

    public void loadLocation() {
        loadLocation(locationStack.pop());
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

    private void checkComments() {
        while(index + 1 < data.length() && data.codePointAt(index) == '/' && data.codePointAt(index + 1) == '/') {
            index += 2;
            lineRow += 2;
            while(index < data.length()) {
                if(data.codePointAt(index++) == '\n') {
                    lineNumber++;
                    lineRow = 0;
                    ignoreWhitespace();
                    break;
                }
                lineRow++;
            }
        }
    }

    public String nextTemplateWord(char[] allowedCharacters) {
        return nextTemplateWord(allowedCharacters, null);
    }

    public String nextTemplateWord(char[] allowedCharacters, char[] operatorCharacters) {
        ignoreWhitespace();
        lastLocation = getLocation();
        if(!hasNext()) return null;
        StringBuilder word = new StringBuilder();
        while(hasNext() && !Character.isWhitespace(data.codePointAt(index))) {
            checkComments();
            if(matchesCharacters(operatorCharacters, data.codePointAt(index))) {
                if(!matchesCharacters(operatorCharacters, word.toString())) {
                    return word.toString();
                }
            }else if(!matchesCharacters(allowedCharacters, data.codePointAt(index))) {
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
        return nextTemplateWord(nonSpecialCharacters, operatorCharacters);
    }

    public String nextLongWord() {
        return nextTemplateWord(validLongWordCharacters);
    }

    public String nextDotWord() {
        return nextTemplateWord(validDotWordCharacters);
    }

    public String seekWord() {
        saveLocation();
        String str = nextWord();
        loadLocation();
        return str;
    }

    public String nextType() {
        return nextTemplateWord(typeCharacters);
    }

    public String nextStringStarted() {
        lastLocation = getLocation();
        if(index + 1 >= data.length()) return null;
        StringBuilder word = new StringBuilder();
        while(index + 1 < data.length()) {
            var character = data.codePointAt(index);
            if(data.codePointAt(index) == '\\') {
                lineRow++;
                index++;
                character = data.codePointAt(index);
                if(character == '\"') {
                    lineRow++;
                    index++;
                    word.append("\"");
                }else if(character == 'n') {
                    lineRow++;
                    index++;
                    word.append('\n');
                }else {
                    System.err.println("Unknown escape code: \\" + character);
                }
                continue;
            }else if(character == '\"') {
                lineRow++;
                index++;
                break;
            }else {
                if(character == '\n') {
                    lineNumber++;
                    lineRow = 0;
                }
                word.append(data.codePointAt(index));
            }
            lineRow++;
            index++;
        }
        return word.toString();
    }

}
