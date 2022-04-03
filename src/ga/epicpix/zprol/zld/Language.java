package ga.epicpix.zprol.zld;

import ga.epicpix.zprol.compiled.PrimitiveType;
import ga.epicpix.zprol.exceptions.ParserException;
import ga.epicpix.zprol.parser.DataParser;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import static ga.epicpix.zprol.parser.DataParser.joinCharacters;
import static ga.epicpix.zprol.parser.DataParser.nonSpecialCharacters;
import static ga.epicpix.zprol.zld.LanguageContextEvent.ContextManipulationOperation;

public class Language {

    public static final ArrayList<String> KEYWORDS = new ArrayList<>();
    public static final HashMap<String, String[]> DEFINES = new HashMap<>();
    public static final ArrayList<LanguageToken> TOKENS = new ArrayList<>();
    public static final HashMap<String, PrimitiveType> TYPES = new HashMap<>();
    public static final HashMap<String, LanguageToken> GHOST_TOKENS = new HashMap<>();
    public static final ArrayList<LanguageContextEvent> CONTEXT_EVENTS = new ArrayList<>();

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

        char[] propertiesCharacters = joinCharacters(nonSpecialCharacters, new char[] {',', '='});

        DataParser parser = new DataParser(fileName, data.toString().split("(\r|\n|\r\n|\n\r)"));
        while(parser.hasNext()) {
            String d = parser.nextWord();
            if(d.equals("keyword")) {
                KEYWORDS.add(parser.nextWord());
            } else if(d.equals("type")) {
                String name = parser.nextWord();
                String properties = parser.nextTemplateWord(propertiesCharacters);
                String descriptor = parser.nextWord();
                char type = 0x8000;
                for(String property : properties.split(",")) {
                    if(!property.contains("=")) property += "=true";
                    String key = property.split("=")[0];
                    String value = property.split("=")[1];

                    switch (key) {
                        case "size" -> {
                            type &= ~0x000f;
                            type |= (int) (Math.log(Integer.parseInt(value) * 2) / Math.log(2));
                        }
                        case "unsigned" -> {
                            type &= ~0x0010;
                            type |= Boolean.parseBoolean(value) ? 0x0010 : 0x0000;
                        }
                        case "pointer" -> {
                            type &= ~0x0020;
                            type |= Boolean.parseBoolean(value) ? 0x0020 : 0x0000;
                        }
                        default -> System.out.println("Unknown key '" + key + "' with value '" + value + "'");
                    }
                }
                TYPES.put(name, new PrimitiveType(type, descriptor, name));
                KEYWORDS.add(name);
            } else if(d.equals("ghost")) {
                String name = parser.nextWord();
                String w = parser.nextWord();
                GHOST_TOKENS.put(w, new LanguageToken("*", name, w));
            } else if(d.equals("tok")) {
                ArrayList<String> tokens = new ArrayList<>();
                String requirement = parser.nextLongWord();
                String name = parser.nextWord();
                while(!parser.checkNewLine()) {
                    tokens.add(convert(parser.nextLongWord(), parser));
                }
                TOKENS.add(new LanguageToken(requirement, name, tokens.toArray(new String[0])));
            } else if(d.equals("context")) {
                if(!parser.nextWord().equals("on")) throw new ParserException("Unknown word after 'context' keyword", parser);
                String on = parser.nextLongWord();
                ContextManipulationOperation manipulation = ContextManipulationOperation.valueOf(parser.nextWord().toUpperCase());
                String context = parser.nextLongWord();
                CONTEXT_EVENTS.add(new LanguageContextEvent(on, manipulation, context));
            } else if(d.equals("define")) {
                String name = parser.nextWord();
                if(DEFINES.get(name) != null) throw new ParserException("Define already defined: " + name, parser);
                ArrayList<String> tokens = new ArrayList<>();
                String w;
                while(!parser.checkNewLine()) {
                    tokens.add(convert(parser.nextLongWord(), parser));
                }
                DEFINES.put(name, tokens.toArray(new String[0]));
            } else {
                throw new ParserException("Unknown language file word: " + d, parser);
            }
        }
    }

}
