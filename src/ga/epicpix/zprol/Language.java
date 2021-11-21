package ga.epicpix.zprol;

import ga.epicpix.zprol.compiled.Type;
import ga.epicpix.zprol.exceptions.ParserException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class Language {

    public static final ArrayList<String> KEYWORDS = new ArrayList<>();
    public static final HashMap<String, ArrayList<String[]>> TOKENS = new HashMap<>();
    public static final HashMap<String, Type> TYPES = new HashMap<>();

    public static void load(String fileName) throws IOException {
        InputStream in = Language.class.getClassLoader().getResourceAsStream(fileName);
        StringBuilder data = new StringBuilder();
        int temp;
        while((temp = in.read()) != -1) {
            data.append((char) temp);
        }

        DataParser parser = new DataParser(fileName, data.toString().split("(\r|\n|\r\n|\n\r)"));
        while(parser.seekWord() != null) {
            String d = parser.nextWord();
            if(d.equals("keyword")) {
                KEYWORDS.add(parser.nextWord());
            } else if(d.equals("type")) {
                String name = parser.nextWord();
                boolean unsigned = Boolean.parseBoolean(parser.nextWord());
                int size = Integer.parseInt(parser.nextWord());
                int sizeId = (int) (Math.log(size) / Math.log(2));
                char id = 0;
                id |= sizeId & 3;              // 0000000000000011
                id |= (unsigned ? 1 : 0) << 2; // 0000000000000100
                TYPES.put(name, new Type(id));
            } else if(d.equals("tok")) {
                String keyword = parser.nextWord();
                if(!KEYWORDS.contains(keyword)) {
                    throw new ParserException("Keyword not defined", parser);
                }
                ArrayList<String> tokens = new ArrayList<>();
                String w;
                while(!"@end@".equals(w = parser.nextLongWord()) && w != null) {
                    if(w.equals(";")) {
                        tokens.add("@line@");
                    }else {
                        tokens.add(w);
                    }
                }
                TOKENS.computeIfAbsent(keyword, k -> new ArrayList<>());
                TOKENS.get(keyword).add(tokens.toArray(new String[0]));
            } else {
                throw new ParserException("Unknown language file word: " + d, parser);
            }
        }
    }

}
