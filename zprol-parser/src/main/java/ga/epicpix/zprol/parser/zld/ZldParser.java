package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.LanguageToken;
import ga.epicpix.zprol.parser.exceptions.ParserException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class ZldParser {

    public static LanguageToken root = null;
    public static final HashMap<String, ArrayList<LanguageToken>> DEFINITIONS = new HashMap<>();

    private static final char[] tokenCharacters = DataParser.joinCharacters(DataParser.nonSpecialCharacters, new char[] {':', '='});

    private static LanguageTokenFragment convert(String w, DataParser parser) {
        LanguageTokenFragment res = null;
        if(w.equals("$")) {
            res = new LexerCallToken(parser.nextTemplateWord(tokenCharacters));
        }else if(w.equals("(")) {
            ArrayList<LanguageTokenFragment> fragmentsList = new ArrayList<>();
            String next;
            while(!(next = parser.nextTemplateWord(tokenCharacters)).equals(")")) {
                fragmentsList.add(convert(next, parser));
            }
            res = new AllToken(fragmentsList.toArray(new LanguageTokenFragment[0]));
        }
        if(parser.hasNext()) {
            var loc = parser.saveLocation();
            int seek = parser.nextChar();
            if(seek == '?') {
                return new OptionalToken(res == null ? convert(w, parser) : res);
            }else if(seek == '&') {
                return new IgnoredToken(res == null ? convert(w, parser) : res);
            }else if(seek == '*') {
                return new MultiToken(res == null ? convert(w, parser) : res);
            }else if(seek == '|') {
                return new OrToken(res == null ? convert(w, parser) : res, convert(parser.nextTemplateWord(tokenCharacters), parser));
            }
            parser.loadLocation(loc);
        }

        if(res != null) {
            return res;
        }

        return new CallToken(w);
    }

    public static void load(String fileName, String data) {
        DataParser parser = new DataParser(fileName, data.split("(\r|\n|\r\n|\n\r)"));
        while(parser.hasNext()) {
            String d = parser.nextWord();
            ArrayList<LanguageTokenFragment> tokens = new ArrayList<>();

            String modifier = parser.seekWord();
            boolean inline = false, merge = false;
            if(modifier.equals("inline")) {
                parser.nextWord();
                inline = true;
            }else if(modifier.equals("merge")) {
                parser.nextWord();
                merge = true;
            }
            if(!parser.nextTemplateWord(tokenCharacters).equals(":=")) {
                throw new ParserException("Expected :=", parser);
            }

            while(parser.hasNext()) {
                tokens.add(convert(parser.nextTemplateWord(tokenCharacters), parser));
                if(parser.checkNewLine()) {
                    break;
                }
            }
            var token = new LanguageToken(d, inline, merge, tokens.toArray(new LanguageTokenFragment[0]));
            if(DEFINITIONS.size() == 0) root = token;
            DEFINITIONS.putIfAbsent(d, new ArrayList<>());
            DEFINITIONS.get(d).add(token);
        }
        if(Boolean.parseBoolean(System.getProperty("SHOW_LANGUAGE_DATA"))) {
            System.out.println("--- Definitions");
            DEFINITIONS.forEach((x, y) -> {
                y.forEach(z -> {
                    String modifier = "";
                    if(z.inline()) modifier = " inline";
                    else if(z.merge()) modifier = " merge";
                    String debugName = Arrays.stream(z.args()).map(LanguageTokenFragment::getDebugName).collect(Collectors.joining(" "));
                    System.out.println(x + modifier + " := " + debugName.replace("\n", "\\n").replace("\t", "\\t"));
                });
            });
        }
    }

}
