package ga.epicpix.zld;

import ga.epicpix.zprol.compiler.operation.LanguageOperator;
import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.LanguageKeyword;
import ga.epicpix.zprol.parser.LanguageToken;
import ga.epicpix.zprol.parser.LanguageTokenFragment;
import ga.epicpix.zprol.parser.tokens.*;
import ga.epicpix.zprol.parser.exceptions.ParserException;
import ga.epicpix.zprol.types.PrimitiveType;
import ga.epicpix.zprol.types.Types;
import ga.epicpix.zprol.utils.SeekIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

public class ZldParser {

    private static final Token[] EMPTY_TOKENS = new Token[0];

    public static final HashMap<String, ArrayList<LanguageToken>> DEFINITIONS = new HashMap<>();

    private static final char[] tagsCharacters = DataParser.joinCharacters(DataParser.nonSpecialCharacters, new char[] {','});
    private static final char[] propertiesCharacters = DataParser.joinCharacters(DataParser.nonSpecialCharacters, new char[] {',', '='});
    private static final char[] tokenCharacters = DataParser.joinCharacters(DataParser.nonSpecialCharacters, new char[] {'@', '%', '=', '>', ':'});
    private static final char[] numberCharacters = DataParser.genCharacters('0', '9');
    private static final char[] operatorCharacters = new char[] {'+', '=', '/', '*', '-', '%', '<', '>', '!', '&'};

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
            return LanguageTokenFragment.createMulti(p -> {
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
            return LanguageTokenFragment.createMulti(p -> {
                var defs = DEFINITIONS.get(use);
                var startLocation = p.getLocation();
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
                    return new Token[]{new NamedToken(use, startLocation, p.getLocation(), p, iterTokens.toArray(EMPTY_TOKENS))};
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
            return LanguageTokenFragment.createMulti(p -> {
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
            StringBuilder debug = new StringBuilder();
            for (int[] c : cs) {
                System.arraycopy(c, 0, allowedCharacters, indx, c.length);
                indx += c.length;
                for(int cc : c) debug.appendCodePoint(cc);
            }
            String debugName = "<" + debug + ">";
            return LanguageTokenFragment.createMulti(p -> {
                var startLocation = p.getLocation();
                p.saveLocation();
                var res = negate ? p.nextCharNot(allowedCharacters) : p.nextChar(allowedCharacters);
                if(res == -1) {
                    p.loadLocation();
                    return null;
                }
                p.discardLocation();
                var endLocation = p.getLocation();
                return new Token[] {new WordToken(Character.toString(res), startLocation, endLocation, p)};
            }, debugName);
        }

        return switch (w) {
            case ";" -> LanguageTokenFragment.createSingle((p) -> LanguageTokenFragment.exactTypeGenerator(";", TokenType.END_LINE, p), w);
            case "," -> LanguageTokenFragment.createSingle((p) -> LanguageTokenFragment.exactTypeGenerator(",", TokenType.COMMA, p), w);
            case "(" -> LanguageTokenFragment.createSingle((p) -> LanguageTokenFragment.exactTypeGenerator("(", TokenType.OPEN, p), w);
            case ")" -> LanguageTokenFragment.createSingle((p) -> LanguageTokenFragment.exactTypeGenerator(")", TokenType.CLOSE, p), w);
            case "{" -> LanguageTokenFragment.createSingle((p) -> LanguageTokenFragment.exactTypeGenerator("{", TokenType.OPEN_SCOPE, p), w);
            case "}" -> LanguageTokenFragment.createSingle((p) -> LanguageTokenFragment.exactTypeGenerator("}", TokenType.CLOSE_SCOPE, p), w);
            default -> keyword ? LanguageTokenFragment.createExactKeywordFragment(w, parser) : LanguageTokenFragment.createExactFragment(w);
        };

    }

    public static boolean hasKeywordTag(String keyword, String tag, boolean def) {
        var k = LanguageKeyword.KEYWORDS.get(keyword);
        return k != null ? k.hasTag(tag) : def;
    }

    public static void load(String fileName, String data) {
        DataParser parser = new DataParser(fileName, data.toString().split("(\r|\n|\r\n|\n\r)"));
        while(parser.hasNext()) {
            String d = parser.nextWord();
            if(d.equals("operator")) {
                int precedence = Integer.parseInt(parser.nextTemplateWord(numberCharacters));
                String operator = parser.nextTemplateWord(operatorCharacters);
                LanguageOperator.OPERATORS.put(operator, new LanguageOperator(operator, precedence));
            }else if(d.equals("keyword")) {
                String[] tags = parser.nextTemplateWord(tagsCharacters).split(",");
                String name = parser.nextWord();
                LanguageKeyword.KEYWORDS.put(name, new LanguageKeyword(name, tags));
            } else if(d.equals("type")) {
                String name = parser.nextWord();
                String properties = parser.nextTemplateWord(propertiesCharacters);
                String descriptor = parser.nextWord();
                int size = 0;
                boolean unsigned = false;
                for(String property : properties.split(",")) {
                    if(!property.contains("=")) property += "=true";
                    String key = property.split("=")[0];
                    String value = property.split("=")[1];

                    switch (key) {
                        case "size" -> size = Integer.parseInt(value);
                        case "unsigned" -> unsigned = Boolean.parseBoolean(value);
                        default -> System.err.println("Unknown type key '" + key + "' with value '" + value + "'");
                    }
                }
                Types.registerType(name, new PrimitiveType(size, unsigned, descriptor, name));
                LanguageKeyword.KEYWORDS.put(name, new LanguageKeyword(name, "type"));
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
                        LanguageToken.TOKENS.add(new LanguageToken(name, false, true, false, false, tokens.toArray(new LanguageTokenFragment[0])));
                        tokens.clear();
                    }
                }
                LanguageToken.TOKENS.add(new LanguageToken(name, false, true, false, false, tokens.toArray(new LanguageTokenFragment[0])));
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
        var copy = new ArrayList<>(tokens);
        tokens.clear();
        String debugName = copy.stream().map(LanguageTokenFragment::getDebugName).collect(Collectors.joining(" "));
        tokens.add(LanguageTokenFragment.createSingle(dataParser -> {
            StringBuilder builder = new StringBuilder();
            var start = dataParser.getLocation();
            for(var c : copy) {
                dataParser.saveLocation();
                var value = c.apply(dataParser);
                if(value == null) {
                    dataParser.loadLocation();
                    return null;
                }
                dataParser.discardLocation();
                for(var s : value) {
                    if(s instanceof WordHolder holder) {
                        builder.append(holder.getWord());
                    }else if(s instanceof NamedToken named) {
                        for(var t : named.tokens) {
                            if(t instanceof WordHolder holder) {
                                builder.append(holder.getWord());
                            }
                        }
                    }
                }
            }
            return new WordToken(builder.toString(), start, dataParser.getLocation(), dataParser);
        }, debugName));
    }

}
