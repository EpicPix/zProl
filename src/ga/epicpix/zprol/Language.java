package ga.epicpix.zprol;

import ga.epicpix.zprol.exceptions.ParserException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class Language {

    public static final ArrayList<String> KEYWORDS = new ArrayList<>();

    public static void load(String fileName) throws IOException {
        InputStream in = Language.class.getClassLoader().getResourceAsStream(fileName);
        StringBuilder data = new StringBuilder();
        int temp;
        while((temp = in.read()) != -1) {
            data.append((char) temp);
        }

        DataParser parser = new DataParser(fileName, data.toString().split("(\r|\n|\r\n|\n\r)+"));
        while(parser.seekWord() != null) {
            String d = parser.nextWord();
            if(d.equals("keyword")) {
                KEYWORDS.add(parser.nextWord());
            }else {
                throw new ParserException("An error occurred while loading a language file", parser);
            }
        }
    }

}
