package ga.epicpix.zprol.zld;

import ga.epicpix.zprol.SeekIterator;
import ga.epicpix.zprol.compiled.PrimitiveType;
import ga.epicpix.zprol.exceptions.ParserException;
import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.Parser;
import ga.epicpix.zprol.parser.tokens.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

import static ga.epicpix.zprol.parser.DataParser.*;
import static ga.epicpix.zprol.zld.LanguageTokenFragment.*;

public class Language {

    private static final Token[] EMPTY_TOKENS = new Token[0];

    public static final HashMap<String, LanguageKeyword> KEYWORDS = new HashMap<>();
    public static final ArrayList<LanguageToken> TOKENS = new ArrayList<>();
    public static final HashMap<String, ArrayList<LanguageToken>> DEFINITIONS = new HashMap<>();
    public static final HashMap<String, PrimitiveType> TYPES = new HashMap<>();
    public static final HashMap<String, LanguageOperator> OPERATORS = new HashMap<>();

    private static final char[] tagsCharacters = joinCharacters(nonSpecialCharacters, new char[] {','});
    private static final char[] propertiesCharacters = joinCharacters(nonSpecialCharacters, new char[] {',', '='});
    private static final char[] tokenCharacters = joinCharacters(nonSpecialCharacters, new char[] {'@', '%', '=', '>', ':'});
    private static final char[] numberCharacters = genCharacters('0', '9');

    private static LanguageTokenFragment convert(boolean keyword, boolean chars, String w, DataParser parser) {
        if(w.startsWith("\\")) {
            w = parser.nextWord();
        } else if(w.equals("{")) {
            ArrayList<LanguageTokenFragment> fragmentsList = new ArrayList<>();
            String next;
            while(!(next = parser.nextTemplateWord(tokenCharacters)).equals("}")) {
                fragmentsList.add(convert(keyword, chars, next, parser));
            }
            LanguageTokenFragment[] fragments = fragmentsList.toArray(new LanguageTokenFragment[0]);
            String debugName = "{" + fragmentsList.stream().map(LanguageTokenFragment::getDebugName).collect(Collectors.joining(" ")) + "}";
            return createMulti(p -> {
                ArrayList<Token> tokens = new ArrayList<>();
                boolean successful = false;

                fLoop: do {
                    p.saveLocation();
                    ArrayList<Token> iterTokens = new ArrayList<>();
                    for (var frag : fragments) {
                        var r = frag.apply(p);
                        if (r == null) {
                            p.loadLocation();
                            if (successful) {
                                break fLoop;
                            } else {
                                return EMPTY_TOKENS;
                            }
                        }
                        Collections.addAll(iterTokens, r);
                    }
                    p.discardLocation();
                    successful = true;
                    tokens.addAll(iterTokens);
                } while(true);
                return tokens.toArray(EMPTY_TOKENS);
            }, debugName);

        }else if(w.equals("$")) {
            String use = parser.nextTemplateWord(tokenCharacters);
            return createMulti(p -> {
                var defs = DEFINITIONS.get(use);
                fLoop: for(LanguageToken def : defs) {
                    p.saveLocation();
                    ArrayList<Token> iterTokens = new ArrayList<>();
                    for (var frag : def.args()) {
                        var currentPos = p.getLocation();
                        var r = frag.apply(p);
                        if (r == null) {
                            if(!currentPos.equals(p.getLocation()) && def.saveable()) {
                                throw new ParserException("Expected " + frag.getDebugName(), p);
                            }
                            p.loadLocation();
                            continue fLoop;
                        }
                        Collections.addAll(iterTokens, r);
                    }
                    p.discardLocation();
                    if(def.clean()) return EMPTY_TOKENS;
                    if(def.inline()) return iterTokens.toArray(EMPTY_TOKENS);
                    return new Token[]{new NamedToken(use, iterTokens.toArray(EMPTY_TOKENS))};
                }
                return null;
            }, "$" + use);
        }else if(w.equals("[")) {
            ArrayList<LanguageTokenFragment> fragmentsList = new ArrayList<>();
            String next;
            while(!(next = parser.nextTemplateWord(tokenCharacters)).equals("]")) {
                fragmentsList.add(convert(keyword, chars, next, parser));
            }
            LanguageTokenFragment[] fragments = fragmentsList.toArray(new LanguageTokenFragment[0]);
            String debugName = "[" + fragmentsList.stream().map(LanguageTokenFragment::getDebugName).collect(Collectors.joining(" ")) + "]";
            return createMulti(p -> {
                p.saveLocation();
                ArrayList<Token> iterTokens = new ArrayList<>();
                for (var frag : fragments) {
                    var r = frag.apply(p);
                    if (r == null) {
                        p.loadLocation();
                        return EMPTY_TOKENS;
                    }
                    Collections.addAll(iterTokens, r);
                }
                p.discardLocation();
                return iterTokens.toArray(EMPTY_TOKENS);
            }, debugName);
        }else if(w.equals("<") && chars) {
            int next;
            ArrayList<Integer> charactersList = new ArrayList<>();
            while((next = parser.nextChar()) != '>') {
                charactersList.add(next);
            }
            SeekIterator<Integer> characters = new SeekIterator<>(charactersList);
            ArrayList<int[]> cs = new ArrayList<>();
            while(characters.hasNext()) {
                int from = characters.next();
                if(characters.hasNext() && characters.seek() == '-') {
                    characters.next(); // skip -
                    int to = characters.next();
                    cs.add(DataParser.genCharacters(from, to));
                }else {
                    if(from == '\\') {
                        int a = characters.next();
                        if(a == 'n') {
                            from = '\n';
                        }else if(a == 't') {
                            from = '\t';
                        }else {
                            from = a;
                        }
                    }
                    cs.add(new int[] {from});
                }
            }
            int[] allowedCharacters = new int[cs.stream().map(arr -> arr.length).reduce(0, Integer::sum)];
            int indx = 0;
            StringBuilder debug = new StringBuilder();
            for (int[] c : cs) {
                System.arraycopy(c, 0, allowedCharacters, indx, c.length);
                indx += c.length;
                for(int cc : c) debug.appendCodePoint(cc);
            }
            String debugName = "<" + debug + ">";
            return createMulti(p -> {
                p.saveLocation();
                var res = p.nextChar(allowedCharacters);
                if(res == -1) {
                    p.loadLocation();
                    return null;
                }
                p.discardLocation();
                return new Token[] {new WordToken(Character.toString(res))};
            }, debugName);
        }

        return switch (w) {
            case ";" -> createExactFragmentType(";", () -> new Token(TokenType.END_LINE));
            case "," -> createExactFragmentType(",", () -> new Token(TokenType.COMMA));
            case "(" -> createExactFragmentType("(", () -> new Token(TokenType.OPEN));
            case ")" -> createExactFragmentType(")", () -> new Token(TokenType.CLOSE));
            case "{" -> createExactFragmentType("{", () -> new Token(TokenType.OPEN_SCOPE));
            case "}" -> createExactFragmentType("}", () -> new Token(TokenType.CLOSE_SCOPE));
            case "@dword@" -> createSingle(p -> {
                String x = validateWord(p.nextDotWord());
                return (x != null && Language.KEYWORDS.get(x) == null) ? new WordToken(x) : null;
            }, "<dword>");
            case "@lword@" -> createSingle(p -> {
                String x = validateWord(p.nextLongWord());
                return (x != null && Language.KEYWORDS.get(x) == null) ? new WordToken(x) : null;
            }, "<lword>");
            case "@number@" -> createSingle(p -> {
                var num = Parser.getInteger(p.nextWord());
                return num == null ? null : new NumberToken(num);
            }, "<number>");
            case "@operator@" -> createSingle(p -> {
                var op = p.nextWord();
                return OPERATORS.get(op) != null ? new OperatorToken(OPERATORS.get(op)) : null;
            }, "<operator>");
            default -> keyword ? createExactKeywordFragment(w, parser) : createExactFragment(w);
        };

    }

    public static PrimitiveType getTypeFromDescriptor(String descriptor) {
        for(PrimitiveType type : TYPES.values()) {
            if(type.descriptor.equals(descriptor)) {
                return type;
            }
        }
        return null;
    }

    public static boolean hasKeywordTag(String keyword, String tag, boolean def) {
        var k = KEYWORDS.get(keyword);
        return k != null ? k.hasTag(tag) : def;
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
            if(d.equals("operator")) {
                int precedence = Integer.parseInt(parser.nextTemplateWord(numberCharacters));
                String operator = parser.nextWord();
                OPERATORS.put(operator, new LanguageOperator(operator, precedence));
            }else if(d.equals("keyword")) {
                String[] tags = parser.nextTemplateWord(tagsCharacters).split(",");
                String name = parser.nextWord();
                KEYWORDS.put(name, new LanguageKeyword(name, tags));
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
                        default -> System.err.println("Unknown type key '" + key + "' with value '" + value + "'");
                    }
                }
                TYPES.put(name, new PrimitiveType(type, descriptor, name));
                KEYWORDS.put(name, new LanguageKeyword(name, "type"));
            } else if(d.equals("tok")) {
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
                        TOKENS.add(new LanguageToken(name, false, true, false, false, tokens.toArray(new LanguageTokenFragment[0])));
                        tokens.clear();
                    }
                }
                TOKENS.add(new LanguageToken(name, false, true, false, false, tokens.toArray(new LanguageTokenFragment[0])));
            } else if(d.equals("def")) {
                ArrayList<LanguageTokenFragment> tokens = new ArrayList<>();
                boolean inline = false, saveable = false, keyword = false, chars = false, clean = false;
                String name = parser.nextWord();
                if(name.equals("inline")) {
                    name = parser.nextWord();
                    inline = true;
                }
                if(name.equals("saveable")) {
                    name = parser.nextWord();
                    saveable = true;
                }
                if(name.equals("keyword")) {
                    name = parser.nextWord();
                    keyword = true;
                }
                if(name.equals("chars")) {
                    name = parser.nextWord();
                    chars = true;
                }
                if(name.equals("clean")) {
                    name = parser.nextWord();
                    clean = true;
                }
                boolean checkWhitespace = false;
                if(parser.seekWord().equals(":")) {
                    parser.nextWord();
                    checkWhitespace = true;
                }
                while(parser.hasNext()) {
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
                        if(chars) {
                            charGenerator(tokens);
                        }
                        DEFINITIONS.putIfAbsent(name, new ArrayList<>());
                        DEFINITIONS.get(name).add(new LanguageToken(name, inline, saveable, keyword, clean, tokens.toArray(new LanguageTokenFragment[0])));
                        tokens.clear();
                    }
                }
                if(chars) {
                    charGenerator(tokens);
                }
                DEFINITIONS.putIfAbsent(name, new ArrayList<>());
                DEFINITIONS.get(name).add(new LanguageToken(name, inline, saveable, keyword, clean, tokens.toArray(new LanguageTokenFragment[0])));
            } else {
                throw new ParserException("Unknown language file word: " + d, parser);
            }
        }
    }

    private static void charGenerator(ArrayList<LanguageTokenFragment> tokens) {
        var copy = new ArrayList<>(tokens);
        tokens.clear();
        String debugName = copy.stream().map(LanguageTokenFragment::getDebugName).collect(Collectors.joining(" "));
        tokens.add(createSingle(dataParser -> {
            StringBuilder builder = new StringBuilder();
            for(var c : copy) {
                dataParser.saveLocation();
                var value = c.apply(dataParser);
                if(value == null) {
                    dataParser.loadLocation();
                    return null;
                }
                dataParser.discardLocation();
                for(var s : value) {
                    builder.append(s.asWordHolder().getWord());
                }
            }
            return new WordToken(builder.toString());
        }, debugName));
    }

}
