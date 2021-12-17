package ga.epicpix.zprol;

import ga.epicpix.zprol.DataParser.SavedLocation;
import ga.epicpix.zprol.compiled.Type;
import ga.epicpix.zprol.exceptions.ParserException;
import ga.epicpix.zprol.tokens.EquationToken;
import ga.epicpix.zprol.tokens.KeywordToken;
import ga.epicpix.zprol.tokens.NumberToken;
import ga.epicpix.zprol.tokens.OperatorToken;
import ga.epicpix.zprol.tokens.ParsedToken;
import ga.epicpix.zprol.tokens.StringToken;
import ga.epicpix.zprol.tokens.Token;
import ga.epicpix.zprol.tokens.TokenType;
import ga.epicpix.zprol.tokens.WordHolder;
import ga.epicpix.zprol.tokens.WordToken;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;

public class Parser {

    private interface TokenGenerator<T> {
        Token generate(T data);
    }

    private interface TokenReader<T> {
        T read(DataParser parser);
    }

    private static String getLanguageDefinition(String[] f, String t) {
        StringBuilder builder = new StringBuilder(t);
        boolean h = false;
        for(int i = 2; i<f.length; i++) {
            String x = f[i];
            if(x.startsWith("%") && x.endsWith("%") && x.length() >= 3) {
                builder.append(x, 1, x.length() - 1);
            }else if(x.startsWith("^")) {
                if(h) builder.append(" ");
                boolean s = x.startsWith("^+");
                builder.append("<").append(x, 1 + (s ? 1 : 0), x.length()).append(s ? "+" : "").append(">");
            }else {
                builder.append(" ").append(x);
            }
            h = x.startsWith("^");
        }
        return builder.toString();
    }

    private static <T> boolean checkToken(DataParser parser, TokenReader<T> reader, TokenGenerator<T> generator, ArrayList<Token> tTokens) {
        T w = reader.read(parser);
        if(w == null) return false;
        tTokens.add(generator.generate(w));
        return true;
    }

    private static boolean checkToken(String check, DataParser parser, TokenGenerator<String> generator, ArrayList<Token> tTokens) {
        String w = parser.nextWord();
        if(!check.equals(w)) return false;
        tTokens.add(generator.generate(w));
        return true;
    }

    public static boolean check(String[] t, DataParser parser, ArrayList<Token> tTokens) {
        ArrayList<Token> added = new ArrayList<>();
        for(int i = 0; i<t.length; i++) {
            String s = t[i];
            if(s.equals("@lword@")) {if(!checkToken(parser, DataParser::nextLongWord, WordToken::new, added)) return false; }
            else if(s.equals("@dword@")) {if(!checkToken(parser, DataParser::nextDotWord, WordToken::new, added)) return false; }
            else if(s.equals("@word@")) {if(!checkToken(parser, DataParser::nextWord, WordToken::new, added)) return false; }
            else if(s.equals("@type@")) {if(!checkToken(parser, DataParser::nextType, WordToken::new, added)) return false; }
            else if(s.equals("@equation@")) {if(!checkToken(parser, Parser::nextEquation, x -> x, added)) return false; }
            else if(s.equals("%;%")) {if(!checkToken(";", parser, (data) -> new Token(TokenType.END_LINE), added)) return false; }
            else if(s.equals("%,%")) {if(!checkToken(",", parser, (data) -> new Token(TokenType.COMMA), added)) return false; }
            else if(s.equals("%(%")) {if(!checkToken("(", parser, (data) -> new Token(TokenType.OPEN), added)) return false; }
            else if(s.equals("%)%")) {if(!checkToken(")", parser, (data) -> new Token(TokenType.CLOSE), added)) return false; }
            else if(s.equals("%{%")) {if(!checkToken("{", parser, (data) -> new Token(TokenType.OPEN_SCOPE), added)) return false; }
            else if(s.equals("%}%")) {if(!checkToken("}", parser, (data) -> new Token(TokenType.CLOSE_SCOPE), added)) return false; }
            else {
                if(s.startsWith("^")) {
                    String[] def = Language.DEFINES.get(s.substring(s.substring(1).startsWith("+") ? 2 : 1));
                    if(s.substring(1).startsWith("+")) {
                        if(!check(def, parser, added)) {
                            return false;
                        }
                        while(true) {
                            SavedLocation location = parser.getSaveLocation();
                            if(!check(def, parser, added)) {
                                parser.loadLocation(location);
                                break;
                            }
                        }
                    }else if(!check(def, parser, added)) {
                        return false;
                    }
                }else if(!checkToken(s, parser, WordToken::new, added)) return false;
            }
        }
        tTokens.addAll(added);
        return true;
    }

    public static Token nextToken(DataParser parser) {
        return getToken(parser, parser.nextWord());
    }

    public static Token getToken(DataParser parser, String word) {
        if(Language.KEYWORDS.contains(word)) throw new ParserException("Keywords not allowed here", parser);
        else if(DataParser.matchesCharacters(DataParser.operatorCharacters, word)) return new OperatorToken(word);
        else if(word.equals(";")) return new Token(TokenType.END_LINE);
        else if(word.equals("(")) return new Token(TokenType.OPEN);
        else if(word.equals(")")) return new Token(TokenType.CLOSE);
        else if(word.equals(",")) return new Token(TokenType.COMMA);
        else if(word.equals(".")) return new Token(TokenType.ACCESSOR);
        else if(word.equals("\"")) return new StringToken(parser.nextStringStarted());
        else if(word.equals("{")) return new Token(TokenType.OPEN_SCOPE);
        else if(word.equals("}")) return new Token(TokenType.CLOSE_SCOPE);

        try {
            return new NumberToken(getInteger(word));
        } catch(NumberFormatException ignored) {}

        return new WordToken(word);
    }

    public static ArrayList<Token> tokenize(String fileName) throws IOException {
        File file = new File(fileName);
        if(!file.exists()) {
            throw new FileNotFoundException(fileName);
        }

        DataParser parser = new DataParser(new File(fileName).getName(), Files.readAllLines(file.toPath()).toArray(new String[0]));

        ArrayList<Token> tokens = new ArrayList<>();

        String word;
        while((word = parser.seekWord()) != null) {
            parser.saveLocation();
            try {
                Token tk = nextToken(parser);
                if(!(tk instanceof WordHolder)) {
                    tokens.add(tk);
                    parser.discardLocation();
                    continue;
                }
            }catch(ParserException ignored) {}
            parser.loadLocation();
            ArrayList<String[]> validOptions = new ArrayList<>();
            for(String[] tok : Language.TOKENS) {
                parser.saveLocation();
                if(check(new String[] {tok[1]}, parser, new ArrayList<>())) {
                    validOptions.add(tok);
                }
                parser.loadLocation();
            }
            ArrayList<Token> preAdded = new ArrayList<>();
            boolean lateStart = false;
            if(Language.KEYWORDS.contains(word)) {
                preAdded.add(new KeywordToken(word));
                lateStart = true;
            }
            boolean valid = false;
            for(String[] tok : validOptions) {
                parser.saveLocation();
                String[] used;
                if(lateStart) {
                    parser.nextWord();
                    used = new String[tok.length - 2];
                    System.arraycopy(tok, 2, used, 0, used.length);
                }else {
                    used = new String[tok.length - 1];
                    System.arraycopy(tok, 1, used, 0, used.length);
                }
                ArrayList<Token> tTokens = new ArrayList<>(preAdded);
                if(check(used, parser, tTokens)) {
                    valid = true;
                    tokens.add(new ParsedToken(tok[0], tTokens));
                    break;
                }
                parser.loadLocation();
            }
            if(!valid) {
                String[] expressions = new String[validOptions.size()];
                String l = parser.nextWord();
                for(int j = 0; j<expressions.length; j++) expressions[j] = "Expected " + getLanguageDefinition(validOptions.get(j), l);
                throw new ParserException(String.join("\n", expressions), parser);
            }
        }
        return tokens;

    }

    public static EquationToken nextEquation(DataParser parser) {
        ArrayList<Token> tokens = new ArrayList<>();
        Token current;
        int open = 0;
        while(true) {
            SavedLocation loc = parser.getSaveLocation();
            current = nextToken(parser);
            if(current.getType() == TokenType.OPEN) open++;
            if(!(current.getType() == TokenType.WORD || current.getType() == TokenType.OPERATOR || current.getType() == TokenType.NUMBER || current.getType() == TokenType.STRING) && open <= 0) {
                parser.loadLocation(loc);
                break;
            }
            if(current.getType() == TokenType.CLOSE) open--;
            tokens.add(current);
        }
        return new EquationToken(tokens);
    }

    public static BigInteger getInteger(String str) {
        int radix = 10;
        if(str.startsWith("0x") || str.startsWith("0X")) {
            radix = 16;
            str = str.substring(2);
        }else if(str.startsWith("0")) {
            if(str.length() >= 2) {
                radix = 8;
                str = str.substring(1);
            }
        }
        return new BigInteger(str, radix);
    }

}
