package ga.epicpix.zprol;

import ga.epicpix.zprol.exceptions.ParserException;
import java.util.ArrayList;
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
        for(char t : chars) if(t == c) return true;
        return false;
    }

    public static boolean matchesCharacters(char[] chars, String c) {
        for(char t : c.toCharArray()) if(!matchesCharacters(chars, t)) return false;
        return true;
    }

    public static final char[] nonSpecialCharacters = joinCharacters(genCharacters('a', 'z'), genCharacters('A', 'Z'), genCharacters('0', '9'), new char[] {'_'});
    public static final char[] operatorCharacters = new char[] {'+', '=', '/', '*', '-', '%', '<', '>', '!', '&'};
    public static final Pattern validLongWordCharacters = Pattern.compile("[a-zA-Z0-9_.+\\-@]*");
    public static final Pattern validDotWordCharacters = Pattern.compile("[a-zA-Z0-9_.]*");

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

    public String nextWord() {
        ignoreWhitespace();
        lastLocation = getLocation();
        if(index >= data.length()) return null;
        StringBuilder word = new StringBuilder();
        char[] cdata = data.toCharArray();
        boolean startType = false;
        while(index < cdata.length && !Character.isWhitespace(cdata[index])) {
            checkComments(cdata);
            boolean matches = matchesCharacters(nonSpecialCharacters, cdata[index]);
            boolean operatorMatches = matchesCharacters(operatorCharacters, word.toString() + cdata[index]);
            if(!operatorMatches && startType) return word.toString();
            if(!matches) {
                if(operatorMatches) {
                    if(word.length() == 0) startType = true;
                } else {
                    if(word.length() == 0) {
                        word.append(cdata[index]);
                        lineRow++;
                        index++;
                    }
                    return word.toString();
                }
            }
            lineRow++;
            word.append(cdata[index]);
            index++;
        }
        return word.toString();
    }

    public String nextLongWord() {
        ignoreWhitespace();
        lastLocation = getLocation();
        if(index >= data.length()) return null;
        StringBuilder word = new StringBuilder();
        char[] cdata = data.toCharArray();
        while(index < cdata.length) {
            if(Character.isWhitespace(cdata[index])) {
                break;
            }
            while(cdata[index] == '/') {
                if(index + 1 < cdata.length) {
                    if(cdata[index + 1] == '/') {
                        index += 2;
                        lineRow += 2;
                        while(index < cdata.length) {
                            if(cdata[index] == '\n') {
                                lineNumber++;
                                lineRow = 0;
                                break;
                            }
                            lineRow++;
                            index++;
                        }
                        ignoreWhitespace();
                    }
                }
            }
            boolean matches = validLongWordCharacters.matcher(cdata[index] + "").matches();
            if(!matches) {
                if(word.length() == 0) {
                    return word.append(cdata[index++]).toString();
                }else {
                    return word.toString();
                }
            }
            lineRow++;
            word.append(cdata[index]);
            index++;
        }
        return word.toString();
    }

    public String nextDotWord() {
        ignoreWhitespace();
        lastLocation = getLocation();
        if(index >= data.length()) return null;
        StringBuilder word = new StringBuilder();
        char[] cdata = data.toCharArray();
        while(index < cdata.length) {
            if(Character.isWhitespace(cdata[index])) {
                break;
            }
            while(cdata[index] == '/') {
                if(index + 1 < cdata.length) {
                    if(cdata[index + 1] == '/') {
                        index += 2;
                        lineRow += 2;
                        while(index < cdata.length) {
                            if(cdata[index] == '\n') {
                                lineNumber++;
                                lineRow = 0;
                                break;
                            }
                            lineRow++;
                            index++;
                        }
                        ignoreWhitespace();
                    }
                }
            }
            boolean matches = validDotWordCharacters.matcher(cdata[index] + "").matches();
            if(!matches) {
                if(word.length() == 0) {
                    return word.append(cdata[index++]).toString();
                }else {
                    return word.toString();
                }
            }
            lineRow++;
            word.append(cdata[index]);
            index++;
        }
        return word.toString();
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

    public ArrayList<ParameterDataType> readParameters() {
        ArrayList<ParameterDataType> parameters = new ArrayList<>();
        lastLocation = getLocation();

        if(!nextWord().equals("(")) {
            throw new ParserException("Start of parameters doesn't have '('", this);
        }

        while(true) {
            if(seekWord().equals(")")) {
                nextWord();
                break;
            }
            String parameterType = nextType();
            String parameterName = nextWord();

            parameters.add(new ParameterDataType(parameterType, parameterName));

            String tmp = seekWord();
            if(tmp.equals(",")) {
                nextWord();
            }else if(tmp.equals(")")) {
                continue;
            }else {
                throw new ParserException("Cannot parse word: " + nextWord(), this);
            }
        }

        return parameters;

    }
}
