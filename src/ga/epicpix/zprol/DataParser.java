package ga.epicpix.zprol;

import java.util.Stack;
import java.util.regex.Pattern;

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

    public static boolean matchesCharacters(char[] chars, char c) {
        if(chars == null) return false;
        for(char t : chars) if(t == c) return true;
        return false;
    }

    public static boolean matchesCharacters(char[] chars, String c) {
        if(chars == null) return false;
        for(char t : c.toCharArray()) if(!matchesCharacters(chars, t)) return false;
        return true;
    }

    public static final char[] nonSpecialCharacters = joinCharacters(genCharacters('a', 'z'), genCharacters('A', 'Z'), genCharacters('0', '9'), new char[] {'_'});
    public static final char[] validDotWordCharacters = joinCharacters(nonSpecialCharacters, new char[] {'.'});
    public static final char[] validLongWordCharacters = joinCharacters(validDotWordCharacters, new char[] {'+', '-', '@'});

    public static final char[] operatorCharacters = new char[] {'+', '=', '/', '*', '-', '%', '<', '>', '!', '&'};

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

    public void ignoreWhitespace() {
        char[] cdata = data.toCharArray();
        while(index + 1 < cdata.length) {
            if(!Character.isWhitespace(cdata[index]) && cdata[index] != '\n') {
                break;
            }
            lineRow++;
            if(cdata[index] == '\n') {
                lineNumber++;
                lineRow = 0;
            }
            index++;
        }
    }

    private void checkComments(char[] cdata) {
        while(cdata[index] == '/') {
            if(index + 1 < cdata.length) {
                if(cdata[index + 1] == '/') {
                    index += 2;
                    lineRow += 2;
                    while(index < cdata.length) {
                        if(cdata[index] == '\n') {
                            lineNumber++;
                            lineRow = 0;
                            index++;
                            break;
                        }
                        lineRow++;
                        index++;
                    }
                    ignoreWhitespace();
                }
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
        char[] cdata = data.toCharArray();
        while(hasNext() && !Character.isWhitespace(cdata[index])) {
            checkComments(cdata);
            boolean matches = matchesCharacters(allowedCharacters, cdata[index]);
            boolean opMatches = matchesCharacters(operatorCharacters, cdata[index]);
            if(opMatches) {
                if(!matchesCharacters(operatorCharacters, word.toString())) {
                    return word.toString();
                }
            }else if(!matches) {
                if(word.length() == 0) {
                    lineRow++;
                    word.append(cdata[index++]);
                }
                return word.toString();
            }else if(matches) {
                if(!matchesCharacters(allowedCharacters, word.toString())) {
                    return word.toString();
                }
            }
            lineRow++;
            word.append(cdata[index++]);
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
        ignoreWhitespace();
        lastLocation = getLocation();
        StringBuilder type = new StringBuilder(nextWord());
        String x = seekWord();
        if(x.equals("(")) {
            while(true) {
                String word = nextWord();
                type.append(word);
                if(word.equals(")")) {
                    break;
                }
            }
        }else if(x.equals("<")) {
            while(true) {
                String word = nextWord();
                type.append(word);
                if(word.equals(">")) {
                    break;
                }
            }
        }
        return type.toString();
    }

    public String nextStringStarted() {
        lastLocation = getLocation();
        if(index + 1 >= data.length()) return null;
        StringBuilder word = new StringBuilder();
        char[] cdata = data.toCharArray();
        while(index + 1 < cdata.length) {
            if(cdata[index] == '\\') {
                lineRow++;
                index++;
                if(cdata[index] == '\"') {
                    lineRow++;
                    index++;
                    word.append("\"");
                }else if(cdata[index] == 'n') {
                    lineRow++;
                    index++;
                    word.append('\n');
                }else {
                    System.err.println("Unknown escape code: \\" + cdata[index]);
                }
                continue;
            }else if(cdata[index] == '\"') {
                lineRow++;
                index++;
                break;
            }else {
                if(cdata[index] == '\n') {
                    lineNumber++;
                    lineRow = 0;
                }
                word.append(cdata[index]);
            }
            lineRow++;
            index++;
        }
        return word.toString();
    }

}
