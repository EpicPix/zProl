package ga.epicpix.zprol;

import java.util.ArrayList;

import static ga.epicpix.zprol.Parser.nonSpecialCharacters;

public class DataParser {

    private String data;
    private int index;

    public DataParser(String... lines) {
        data = String.join("\n", lines);
    }

    public void ignoreWhitespace() {
        char[] cdata = data.toCharArray();
        while(index + 1 < cdata.length) {
            if(!Character.isWhitespace(cdata[index])) {
                break;
            }
            index++;
        }
    }

    public String nextWord() {
        ignoreWhitespace();
        if(index >= data.length()) return null;
        StringBuilder word = new StringBuilder();
        char[] cdata = data.toCharArray();
        while(index < cdata.length) {
            if(Character.isWhitespace(cdata[index])) {
                break;
            }
            boolean matches = nonSpecialCharacters.matcher(cdata[index] + "").matches();
            if(!matches) {
                if(word.length() == 0) {
                    word.append(cdata[index]);
                    index++;
                }
                return word.toString();
            }
            word.append(cdata[index]);
            index++;
        }
        return word.toString();
    }

    public String seekWord() {
        int start = index;
        String str = nextWord();
        index = start;
        return str;
    }

    public String nextType() {
        StringBuilder type = new StringBuilder(nextWord());
        if(seekWord().equals("(")) {
            while(true) {
                String word = nextWord();
                type.append(word);
                if(word.equals(")")) {
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
                index++;
                if(cdata[index] == '\"') {
                    index++;
                    word.append("\"");
                }else if(cdata[index] == 'n') {
                    index++;
                    word.append('\n');
                }else {
                    System.err.println("Unknown escape code: \\" + cdata[index]);
                }
                continue;
            }else if(cdata[index] == '\"') {
                index++;
                break;
            }else {
                word.append(cdata[index]);
            }
            index++;
        }
        return word.toString();
    }

    public ArrayList<ParameterDataType> readParameters() {
        ArrayList<ParameterDataType> parameters = new ArrayList<>();

        if(!nextWord().equals("(")) {
            throw new RuntimeException("Start of parameters doesn't have '('");
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
                throw new RuntimeException("Cannot parse word: " + seekWord());
            }
        }

        return parameters;

    }
}
