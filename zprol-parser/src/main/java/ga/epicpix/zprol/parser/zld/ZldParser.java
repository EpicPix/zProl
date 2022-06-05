package ga.epicpix.zprol.parser.zld;

import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.LanguageToken;
import ga.epicpix.zprol.parser.tokens.*;
import ga.epicpix.zprol.parser.exceptions.ParserException;
import ga.epicpix.zprol.utils.SeekIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class ZldParser {

    public static final HashMap<String, ArrayList<LanguageToken>> DEFINITIONS = new HashMap<>();

    private static final char[] tokenCharacters = DataParser.joinCharacters(DataParser.nonSpecialCharacters, new char[] {'@', '%', '=', '>', ':'});

    private static LanguageTokenFragment convert(boolean keyword, boolean chars, String w, DataParser parser) {
        if(w.startsWith("\\")) {
            w = parser.nextWord();
        } else if(w.equals("{")) {
            ArrayList<LanguageTokenFragment> fragmentsList = new ArrayList<>();
            String next;
            while(!(next = parser.nextTemplateWord(tokenCharacters)).equals("}")) {
                fragmentsList.add(convert(keyword, chars, next, parser));
            }
            return new MultiToken(fragmentsList.toArray(new LanguageTokenFragment[0]));
        }else if(w.equals("$")) {
            return new CallToken(parser.nextTemplateWord(tokenCharacters));
        }else if(w.equals("[")) {
            ArrayList<LanguageTokenFragment> fragmentsList = new ArrayList<>();
            String next;
            while(!(next = parser.nextTemplateWord(tokenCharacters)).equals("]")) {
                fragmentsList.add(convert(keyword, chars, next, parser));
            }
            return new OptionalToken(fragmentsList.toArray(new LanguageTokenFragment[0]));
        }else if(w.equals("<") && chars) {
            int next;
            ArrayList<Integer> charactersList = new ArrayList<>();
            while((next = parser.nextChar()) != '>') {
                if(next == '\\') {
                    int a = parser.nextChar();
                    if(a == 'n') {
                        next = '\n';
                    }else if(a == 't') {
                        next = '\t';
                    }else {
                        next = a;
                    }
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
            return new CharsToken(negate, allowedCharacters);
        }

        return switch (w) {
            case ";" -> new LanguageTokenFragment(new ExactTypeTokenReader(";", TokenType.END_LINE), w);
            case "," -> new LanguageTokenFragment(new ExactTypeTokenReader(",", TokenType.COMMA), w);
            case "(" -> new LanguageTokenFragment(new ExactTypeTokenReader("(", TokenType.OPEN), w);
            case ")" -> new LanguageTokenFragment(new ExactTypeTokenReader(")", TokenType.CLOSE), w);
            case "{" -> new LanguageTokenFragment(new ExactTypeTokenReader("{", TokenType.OPEN_SCOPE), w);
            case "}" -> new LanguageTokenFragment(new ExactTypeTokenReader("}", TokenType.CLOSE_SCOPE), w);
            default -> new LanguageTokenFragment(keyword ? new ExactKeywordTokenReader(w, parser) : new ExactWordTokenReader(w), w);
        };

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
                    tokens.add(convert(false, false, parser.nextTemplateWord(tokenCharacters), parser));
                    if(!checkWhitespace) {
                        if(parser.checkNewLine()) {
                            break;
                        }
                        continue;
                    }
                    if(parser.checkNewLine()) {
                        if(parser.seekCharacter() != ' ') {
                            break;
                        }
                        LanguageToken.TOKENS.add(new LanguageToken(name, false, false, false, false, tokens.toArray(new LanguageTokenFragment[0])));
                        tokens.clear();
                    }
                }
                LanguageToken.TOKENS.add(new LanguageToken(name, false, false, false, false, tokens.toArray(new LanguageTokenFragment[0])));
            } else if(d.equals("def")) {
                ArrayList<LanguageTokenFragment> tokens = new ArrayList<>();
                String name = parser.nextWord();
                var inline = false;
                if(name.equals("inline")) {
                    name = parser.nextWord();
                    inline = true;
                }
                var keyword = false;
                if(name.equals("keyword")) {
                    name = parser.nextWord();
                    keyword = true;
                }
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
                var merge = false;
                if(name.equals("merge")) {
                    name = parser.nextWord();
                    merge = true;
                }
                var error = false;
                if(name.equals("error")) {
                    name = parser.nextWord();
                    error = true;
                }
                boolean checkWhitespace = false;
                if(parser.seekWord().equals(":")) {
                    parser.nextWord();
                    checkWhitespace = true;
                }
                StringBuilder errorMessage = new StringBuilder();
                boolean captureErrorMessage = true;
                while(parser.hasNext()) {
                    if(error) {
                        if(captureErrorMessage) {
                            if (parser.seekTemplatedWord(new char[]{'<', '/', '>'}).equals("</>")) {
                                parser.nextTemplateWord(new char[]{'<', '/', '>'});
                                captureErrorMessage = false;
                                continue;
                            }
                            errorMessage.appendCodePoint(parser.nextChar());
                            continue;
                        }
                    }
                    tokens.add(convert(keyword, chars, parser.nextTemplateWord(tokenCharacters), parser));
                    if(!checkWhitespace) {
                        if(parser.checkNewLine()) {
                            break;
                        }
                        continue;
                    }
                    if(parser.checkNewLine()) {
                        if(parser.seekCharacter() != ' ') {
                            break;
                        }
                        if(chars && !clean) {
                            charGenerator(tokens);
                        }
                        DEFINITIONS.putIfAbsent(name, new ArrayList<>());
                        if(error) {
                            tokens.add(new ErrorToken(errorMessage.toString().trim()));
                        }
                        DEFINITIONS.get(name).add(new LanguageToken(name, inline, keyword, clean, merge, tokens.toArray(new LanguageTokenFragment[0])));
                        tokens.clear();
                        captureErrorMessage = true;
                        errorMessage.setLength(0);
                    }
                }
                if(chars && !clean) {
                    charGenerator(tokens);
                }
                DEFINITIONS.putIfAbsent(name, new ArrayList<>());
                if(error) {
                    tokens.add(new ErrorToken(errorMessage.toString().trim()));
                }
                DEFINITIONS.get(name).add(new LanguageToken(name, inline, keyword, clean, merge, tokens.toArray(new LanguageTokenFragment[0])));
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

    private static void charGenerator(ArrayList<LanguageTokenFragment> tokens) {
        var copy = tokens.toArray(new LanguageTokenFragment[0]);
        tokens.clear();
        tokens.add(new CharsWordToken(copy));
    }

}
