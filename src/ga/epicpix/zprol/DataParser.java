package ga.epicpix.zprol;

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
        StringBuilder word = new StringBuilder();
        char[] cdata = data.toCharArray();
        while(index + 1 < cdata.length) {
            if(Character.isWhitespace(cdata[index])) {
                break;
            }
            for(char special : Parser.specialCharacters) {
                if(cdata[index] == special) {
                    return word.toString();
                }
            }
            word.append(cdata[index]);
            index++;
        }
        return word.toString();
    }

}
