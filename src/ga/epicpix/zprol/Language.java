package ga.epicpix.zprol;

import ga.epicpix.zprol.compiled.Type;
import ga.epicpix.zprol.exceptions.ParserException;
import ga.epicpix.zprol.parser.DataParser;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class Language {

    public static final ArrayList<String> KEYWORDS = new ArrayList<>();
    public static final HashMap<String, String[]> DEFINES = new HashMap<>();
    public static final ArrayList<String[]> TOKENS = new ArrayList<>();
    public static final HashMap<String, Type> TYPES = new HashMap<>();

    private static String convert(String w, DataParser parser) {
        if(w.startsWith("\\")) return w.substring(1);
        else if(w.equals("^")) return "^" + parser.nextLongWord();
        else if(w.equals(";")) return "%;%";
        else if(w.equals(",")) return "%,%";
        else if(w.equals("(")) return "%(%";
        else if(w.equals(")")) return "%)%";
        else if(w.equals("{")) return "%{%";
        else if(w.equals("}")) return "%}%";
        return w;
    }

    public static void load(String fileName) throws IOException {
        InputStream in = Language.class.getClassLoader().getResourceAsStream(fileName);
        StringBuilder data = new StringBuilder();
        int temp;
        while((temp = in.read()) != -1) {
            data.append((char) temp);
        }

        DataParser parser = new DataParser(fileName, data.toString().split("(\r|\n|\r\n|\n\r)"));
        while(parser.hasNext()) {
            String d = parser.nextWord();
            if(d.equals("keyword")) {
                KEYWORDS.add(parser.nextWord());
            } else if(d.equals("type")) {
                String name = parser.nextWord();
                boolean unsigned = Boolean.parseBoolean(parser.nextWord());
                int size = Integer.parseInt(parser.nextWord());
                boolean pointer = Boolean.parseBoolean(parser.nextWord());

                int sizeId = (int) (Math.log(size * 2) / Math.log(2));
                char id = 0;
                id |= sizeId & 7;              // 0000000000000111
                id |= (unsigned ? 1 : 0) << 3; // 0000000000001000
                id |= (pointer ? 1 : 0) << 4;  // 0000000000010000
                TYPES.put(name, new Type(id, name));
                KEYWORDS.add(name);
            } else if(d.equals("tok")) {
                ArrayList<String> tokens = new ArrayList<>();
                String w;
                while(!"@end@".equals(w = parser.nextLongWord()) && w != null) {
                    tokens.add(convert(w, parser));
                }
                TOKENS.add(tokens.toArray(new String[0]));
            } else if(d.equals("define")) {
                String name = parser.nextWord();
                if(DEFINES.get(name) != null) throw new ParserException("Define already defined: " + name, parser);
                ArrayList<String> tokens = new ArrayList<>();
                String w;
                while(!"@end@".equals(w = parser.nextLongWord()) && w != null) {
                    tokens.add(convert(w, parser));
                }
                DEFINES.put(name, tokens.toArray(new String[0]));
            } else {
                throw new ParserException("Unknown language file word: " + d, parser);
            }
        }
    }

}
