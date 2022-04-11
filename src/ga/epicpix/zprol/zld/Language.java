package ga.epicpix.zprol.zld;

import ga.epicpix.zprol.compiled.PrimitiveType;
import ga.epicpix.zprol.exceptions.ParserException;
import ga.epicpix.zprol.parser.DataParser;
import ga.epicpix.zprol.parser.Parser;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.TokenType;
import ga.epicpix.zprol.parser.tokens.WordToken;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

import static ga.epicpix.zprol.parser.DataParser.*;
import static ga.epicpix.zprol.zld.LanguageTokenFragment.*;

public class Language {

    public static final HashMap<String, LanguageKeyword> KEYWORDS = new HashMap<>();
    public static final ArrayList<LanguageToken> TOKENS = new ArrayList<>();
    public static final HashMap<String, PrimitiveType> TYPES = new HashMap<>();

    private static final char[] tagsCharacters = joinCharacters(nonSpecialCharacters, new char[] {','});
    private static final char[] propertiesCharacters = joinCharacters(nonSpecialCharacters, new char[] {',', '='});
    private static final char[] tokenCharacters = joinCharacters(nonSpecialCharacters, new char[] {'@', '%'});

    private static LanguageTokenFragment convert(String w, DataParser parser) {
        if(w.startsWith("\\"))
            return createExactFragment(w.substring(1));

        if(w.equals("^")) {
            String next = parser.nextTemplateWord(tokenCharacters);
            boolean multi = false;
            boolean failable = false;
            boolean m = false;
            String name = null;
            if(next.equals("+")) {
                multi = true;
                next = parser.nextTemplateWord(tokenCharacters);
            }
            if(next.equals("*")) {
                failable = true;
                next = parser.nextTemplateWord(tokenCharacters);
            }
            if(next.equals("?")) {
                m = true;
                next = parser.nextTemplateWord(tokenCharacters);
            }
            if(next.equals("-")) {
                name = parser.nextWord();
                next = parser.nextTemplateWord(tokenCharacters);
            }
            if(next.equals("{")) {
                ArrayList<LanguageTokenFragment> fragmentsList = new ArrayList<>();
                while(!(next = parser.nextTemplateWord(tokenCharacters)).equals("}")) {
                    fragmentsList.add(convert(next, parser));
                }
                LanguageTokenFragment[] fragments = fragmentsList.toArray(new LanguageTokenFragment[0]);
                String debugName = "^" + (multi ? "+" : "") + (failable ? "*" : "") + (m ? "?" : "") + (name != null ? "-" + name : "") + "{" + fragmentsList.stream().map(LanguageTokenFragment::getDebugName).collect(Collectors.joining(" ")) + "}";
                final boolean isMulti = multi;
                final boolean isFailable = failable || m;
                final boolean isM = m;
                final String getName = name;
                return createMulti(p -> {
                    ArrayList<Token> tokens = new ArrayList<>();
                    boolean successful = false;

                    fLoop: do {
                        p.saveLocation();
                        ArrayList<Token> iterTokens = new ArrayList<>();
                        for (var frag : fragments) {
                            var r = frag.getTokenReader().apply(p);
                            if (r == null) {
                                p.loadLocation();
                                if (successful) {
                                    break fLoop;
                                } else {
                                    return isFailable ? new Token[0] : null;
                                }
                            }
                            Collections.addAll(iterTokens, r);
                        }
                        p.discardLocation();
                        successful = true;
                        if(getName == null) {
                            tokens.addAll(iterTokens);
                        }else {
                            tokens.add(new NamedToken(getName, iterTokens.toArray(new Token[0])));
                        }
                        if(isM) {
                            break;
                        }
                    } while(isMulti);
                    return tokens.toArray(new Token[0]);
                }, debugName);
            }
        }

        return switch (w) {
            case ";" -> createExactFragmentType(";", () -> new Token(TokenType.END_LINE));
            case "," -> createExactFragmentType(",", () -> new Token(TokenType.COMMA));
            case "(" -> createExactFragmentType("(", () -> new Token(TokenType.OPEN));
            case ")" -> createExactFragmentType(")", () -> new Token(TokenType.CLOSE));
            case "{" -> createExactFragmentType("{", () -> new Token(TokenType.OPEN_SCOPE));
            case "}" -> createExactFragmentType("}", () -> new Token(TokenType.CLOSE_SCOPE));
            case "@word@" -> createSingle(p -> {
                String x = validateWord(p.nextWord());
                return (x != null && Language.KEYWORDS.get(x) == null) ? new WordToken(x) : null;
            }, "<word>");
            case "@dword@" -> createSingle(p -> {
                String x = validateWord(p.nextDotWord());
                return (x != null && Language.KEYWORDS.get(x) == null) ? new WordToken(x) : null;
            }, "<dword>");
            case "@lword@" -> createSingle(p -> {
                String x = validateWord(p.nextLongWord());
                return (x != null && Language.KEYWORDS.get(x) == null) ? new WordToken(x) : null;
            }, "<lword>");
            case "@type@" -> createSingle(p -> {
                String x = validateWord(p.nextWord());
                return (x != null && Language.hasKeywordTag(x, "type", true)) ? new WordToken(x) : null;
            }, "<type>");
            case "@equation@" -> createSingle(Parser::nextEquation, "<equation>");
            default -> createExactFragment(w);
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
            if(d.equals("keyword")) {
                String[] tags = parser.nextTemplateWord(tagsCharacters).split("\\.");
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
                while(!parser.checkNewLine() && parser.hasNext()) {
                    tokens.add(convert(parser.nextTemplateWord(tokenCharacters), parser));
                }
                TOKENS.add(new LanguageToken(name, tokens.toArray(new LanguageTokenFragment[0])));
            } else {
                throw new ParserException("Unknown language file word: " + d, parser);
            }
        }
    }

}
