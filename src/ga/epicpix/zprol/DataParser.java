package ga.epicpix.zprol;

import ga.epicpix.zprol.exceptions.ParserException;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class DataParser {

    public static final Pattern nonSpecialCharacters = Pattern.compile("[a-zA-Z0-9_]");
    public static final Pattern operatorCharacters = Pattern.compile("[+=/*\\-%<>!&]*");

    private String data;
    private String[] lines;
    private int index;

    private int lineNumber;
    private int lineRow;

    public DataParser(String... lines) {
        this.lines = lines;
        data = String.join("\n", lines);
    }

    public String[] getLines() {
        return lines;
    }

    public ParserLocation getLocation() {
        return new ParserLocation(lineNumber, lineRow);
    }

    public void ignoreWhitespace() {
        char[] cdata = data.toCharArray();
        while(index + 1 < cdata.length) {
            if(cdata[index] == '\n') {
                lineNumber++;
                lineRow = 0;
            }
            if(!Character.isWhitespace(cdata[index])) {
                break;
            }
            lineRow++;
            index++;
        }
    }

    public String nextWord() {
        ignoreWhitespace();
        if(index >= data.length()) return null;
        StringBuilder word = new StringBuilder();
        char[] cdata = data.toCharArray();
        boolean startType = false;
        while(index < cdata.length) {
            if(Character.isWhitespace(cdata[index])) {
                break;
            }
            if(cdata[index] == '/') {
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
            boolean matches = nonSpecialCharacters.matcher(cdata[index] + "").matches();
            boolean operatorMatches = operatorCharacters.matcher(word.toString() + cdata[index]).matches();
            if(!operatorMatches && startType) {
                return word.toString();
            }
            if(!matches) {
                if(operatorMatches) {
                    if(word.length() == 0) {
                        startType = true;
                    }
                }else {
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

    public String seekWord() {
        int start = index;
        int currentLine = lineNumber;
        int currentLineRow = lineRow;
        String str = nextWord();
        index = start;
        lineNumber = currentLine;
        lineRow = currentLineRow;
        return str;
    }

    public String nextType() {
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

        ParserLocation loc = getLocation();
        if(!nextWord().equals("(")) {
            throw new ParserException("Start of parameters doesn't have '('", this, loc);
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
                loc = getLocation();
                throw new ParserException("Cannot parse word: " + nextWord(), this, loc);
            }
        }

        return parameters;

    }
}
