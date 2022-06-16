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

    public static final HashMap<String, ArrayList<LanguageToken>> DEFINITIONS = new HashMap<>();

    private static final char[] tokenCharacters = DataParser.joinCharacters(DataParser.nonSpecialCharacters, new char[] {'@', '%', '=', '>', ':'});

    private static LanguageLexerTokenFragment convertLexer(String w, DataParser parser) {
        if(w.equals("*")) {
            LanguageLexerTokenFragment fragment = convertLexer(parser.nextTemplateWord(tokenCharacters), parser);
            return new LanguageLexerTokenFragment(true, fragment.isNegate(), fragment.getCharacters());
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
        if(w.startsWith("\\")) {
            w = parser.nextWord();
        } else if(w.equals("{")) {
            ArrayList<LanguageTokenFragment> fragmentsList = new ArrayList<>();
            String next;
            while(!(next = parser.nextTemplateWord(tokenCharacters)).equals("}")) {
                fragmentsList.add(convert(next, parser));
            }
            return new MultiToken(fragmentsList.toArray(new LanguageTokenFragment[0]));
        }else if(w.equals("$")) {
            String next = parser.nextTemplateWord(tokenCharacters);
            if(next.equals("$")) {
                return new LexerCallToken(parser.nextTemplateWord(tokenCharacters));
            }else {
                return new CallToken(next);
            }
        }else if(w.equals("[")) {
            ArrayList<LanguageTokenFragment> fragmentsList = new ArrayList<>();
            String next;
            while(!(next = parser.nextTemplateWord(tokenCharacters)).equals("]")) {
                fragmentsList.add(convert(next, parser));
            }
            return new OptionalToken(fragmentsList.toArray(new LanguageTokenFragment[0]));
        }

        throw new ParserException(w, parser);
    }

    public static void load(String fileName, String data) {
        DataParser parser = new DataParser(fileName, data.split("(\r|\n|\r\n|\n\r)"));
        while(parser.hasNext()) {
            String d = parser.nextWord();
            if(d.equals("tok")) {
                ArrayList<LanguageTokenFragment> tokens = new ArrayList<>();
                String name = parser.nextWord();
                boolean checkWhitespace = false;
                if(parser.seekWord().equals(":")) {
                    parser.nextWord();
                    checkWhitespace = true;
                }
                while(parser.hasNext()) {
                    tokens.add(convert(parser.nextTemplateWord(tokenCharacters), parser));
                    if(!checkWhitespace) {
                        if(parser.checkNewLine()) {
                            break;
                        }
                        continue;
                    }
                    if(parser.checkNewLine()) {
                        if(parser.seekNextCharacter() != ' ') {
                            break;
                        }
                        LanguageToken.TOKENS.add(new LanguageToken(name, false, false, tokens.toArray(new LanguageTokenFragment[0])));
                        tokens.clear();
                    }
                }
                LanguageToken.TOKENS.add(new LanguageToken(name, false, false, tokens.toArray(new LanguageTokenFragment[0])));
            } else if(d.equals("def")) {
                ArrayList<LanguageTokenFragment> tokens = new ArrayList<>();
                String name = parser.nextWord();
                var inline = false;
                if(name.equals("inline")) {
                    name = parser.nextWord();
                    inline = true;
                }
                var merge = false;
                if(name.equals("merge")) {
                    name = parser.nextWord();
                    merge = true;
                }
                boolean checkWhitespace = false;
                if(parser.seekWord().equals(":")) {
                    parser.nextWord();
                    checkWhitespace = true;
                }
                while(parser.hasNext()) {
                    tokens.add(convert(parser.nextTemplateWord(tokenCharacters), parser));
                    if(!checkWhitespace) {
                        if(parser.checkNewLine()) {
                            break;
                        }
                        continue;
                    }
                    if(parser.checkNewLine()) {
                        if(parser.seekNextCharacter() != ' ') {
                            break;
                        }
                        DEFINITIONS.putIfAbsent(name, new ArrayList<>());
                        DEFINITIONS.get(name).add(new LanguageToken(name, inline, merge, tokens.toArray(new LanguageTokenFragment[0])));
                        tokens.clear();
                    }
                }
                DEFINITIONS.putIfAbsent(name, new ArrayList<>());
                DEFINITIONS.get(name).add(new LanguageToken(name, inline, merge, tokens.toArray(new LanguageTokenFragment[0])));
            } else if(d.equals("lex")) {
                var fragments = new ArrayList<LanguageLexerTokenFragment>();
                var name = parser.nextWord();
                var chars = false;
                if(name.equals("chars")) {
                    name = parser.nextWord();
                    chars = true;
                }
                var clean = false;
                if(name.equals("clean")) {
                    name = parser.nextWord();
                    clean = true;
                }

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
                throw new ParserException("Unknown grammar file word: " + d, parser);
            }
        }
        if(Boolean.parseBoolean(System.getProperty("SHOW_LANGUAGE_DATA"))) {
            System.out.println("--- Definitions");
            DEFINITIONS.forEach((x, y) -> {
                System.out.println("  " + x);
                y.forEach(z -> {
                    String debugName = Arrays.stream(z.args()).map(LanguageTokenFragment::getDebugName).collect(Collectors.joining(" "));
                    System.out.println("    " + debugName.replace("\n", "\\n").replace("\t", "\\t"));
                });
            });
        }
    }

}
