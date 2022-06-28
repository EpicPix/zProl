package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.LanguageToken;
import ga.epicpix.zprol.parser.lexer.LanguageLexerToken;
import ga.epicpix.zprol.parser.lexer.LanguageLexerTokenFragment;
import ga.epicpix.zprol.parser.exceptions.ParserException;
import ga.epicpix.zprol.utils.SeekIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import static ga.epicpix.zprol.parser.lexer.LanguageLexerToken.LEXER_TOKENS;

public class ZldParser {

    public static LanguageToken root = null;
    public static final HashMap<String, ArrayList<LanguageToken>> DEFINITIONS = new HashMap<>();

    private static final char[] tokenCharacters = DataParser.joinCharacters(DataParser.nonSpecialCharacters, new char[] {':', '='});

    private static LanguageLexerTokenFragment convertLexer(String w, DataParser parser) {
        if(w.equals("*")) {
            LanguageLexerTokenFragment fragment = convertLexer(parser.nextTemplateWord(tokenCharacters), parser);
            return new LanguageLexerTokenFragment(true, fragment.negate(), fragment.characters());
        }else if(w.equals("<")) {
            int next;
            ArrayList<Integer> charactersList = new ArrayList<>();
            while((next = parser.nextChar()) != '>') {
                if(next == '\\') {
                    int a = parser.nextChar();
                    next = switch(a) {
                        case 'n' -> '\n';
                        case 't' -> '\t';
                        default -> a;
                    };
                }
                charactersList.add(next);
            }
            SeekIterator<Integer> characters = new SeekIterator<>(charactersList);
            ArrayList<int[]> cs = new ArrayList<>();
            boolean negate = characters.hasNext() && characters.seek() == '^' && characters.next() == '^';
            while(characters.hasNext()) {
                int from = characters.next();
                if(characters.hasNext() && characters.seek() == '-') {
                    characters.next(); // skip -
                    int to = characters.next();
                    cs.add(DataParser.genCharacters(from, to));
                }else {
                    cs.add(new int[] {from});
                }
            }
            int[] allowedCharacters = new int[cs.stream().map(arr -> arr.length).reduce(0, Integer::sum)];
            int indx = 0;
            for (int[] c : cs) {
                System.arraycopy(c, 0, allowedCharacters, indx, c.length);
                indx += c.length;
            }
            return new LanguageLexerTokenFragment(false, negate, allowedCharacters);
        }

        throw new ParserException(w, parser);
    }

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
        }else if(w.equals("'")) {
            StringBuilder name = new StringBuilder();
            int next;
            while((next = parser.nextChar()) != '\'') {
                name.appendCodePoint(next);
            }
            nextLT: for(var lt : LEXER_TOKENS) {
                if(lt.clean()) continue;
                StringBuilder str = new StringBuilder();
                for(var arg : lt.args()) {
                    if(arg.multi() || arg.negate() || arg.characters().length != 1) continue nextLT;
                    str.appendCodePoint(arg.characters()[0]);
                }
                if(name.toString().equals(str.toString())) {
                    res = new ExactLexerCallToken(lt, str.toString());
                    break;
                }
            }
            if(res == null) throw new ParserException("Cannot find exact lexer token: " + name, parser);
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
            if(d.equals("$")) {
                String name = parser.nextWord();
                boolean chars = false, clean = false;
                if(parser.seekWord().equals("chars")) {
                    parser.nextWord();
                    chars = true;
                }
                if(parser.seekWord().equals("clean")) {
                    parser.nextWord();
                    clean = true;
                }
                var fragments = new ArrayList<LanguageLexerTokenFragment>();

                if(parser.seekCharacter() == ' ') parser.nextChar();

                while(parser.hasNext()) {
                    if(chars) {
                        fragments.add(convertLexer(parser.nextTemplateWord(tokenCharacters), parser));
                    }else {
                        fragments.add(new LanguageLexerTokenFragment(false, false, parser.nextChar()));
                    }
                    if(parser.checkNewLine()) {
                        break;
                    }
                }
                LEXER_TOKENS.add(new LanguageLexerToken(name, clean, fragments.toArray(new LanguageLexerTokenFragment[0])));
            } else {
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
