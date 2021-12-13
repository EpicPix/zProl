package ga.epicpix.zprol;

import ga.epicpix.zprol.DataParser.SavedLocation;
import ga.epicpix.zprol.compiled.Type;
import ga.epicpix.zprol.exceptions.ParserException;
import ga.epicpix.zprol.tokens.EquationToken;
import ga.epicpix.zprol.tokens.FieldToken;
import ga.epicpix.zprol.tokens.FunctionToken;
import ga.epicpix.zprol.tokens.KeywordToken;
import ga.epicpix.zprol.tokens.LongWordToken;
import ga.epicpix.zprol.tokens.NumberToken;
import ga.epicpix.zprol.tokens.ObjectToken;
import ga.epicpix.zprol.tokens.OperatorToken;
import ga.epicpix.zprol.tokens.StringToken;
import ga.epicpix.zprol.tokens.Token;
import ga.epicpix.zprol.tokens.TokenType;
import ga.epicpix.zprol.tokens.TypeToken;
import ga.epicpix.zprol.tokens.WordToken;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

public class Parser {

    private interface TokenGenerator<T> {
        Token generate(T data);
    }

    private interface TokenReader<T> {
        T read(DataParser parser);
    }

    private static <T> boolean checkToken(String friendlyName, DataParser parser, TokenReader<T> reader, TokenGenerator<T> generator, boolean last, ArrayList<Token> tTokens) {
        T w = reader.read(parser);
        if(w == null) {
            if(last) throw new ParserException("Expected " + friendlyName, parser);
            else return false;
        }
        tTokens.add(generator.generate(w));
        return true;
    }

    private static boolean checkToken(String check, DataParser parser, TokenGenerator<String> generator, boolean last, ArrayList<Token> tTokens) {
        String w = parser.nextWord();
        if(!check.equals(w)) {
            if(last) throw new ParserException("Expected '" + check + "'", parser);
            else return false;
        }
        tTokens.add(generator.generate(w));
        return true;
    }

    public static boolean check(String[] t, DataParser parser, boolean last, ArrayList<Token> tTokens) {
        ArrayList<Token> added = new ArrayList<>();
        for(String s : t) {
            if(s.equals("@lword@")) {if(!checkToken("long word", parser, DataParser::nextLongWord, LongWordToken::new, last, added)) return false; }
            else if(s.equals("@word@")) {if(!checkToken("word", parser, DataParser::nextWord, WordToken::new, last, added)) return false; }
            else if(s.equals("@type@")) {if(!checkToken("type", parser, DataParser::nextType, WordToken::new, last, added)) return false; }
            else if(s.equals("@equation@")) {if(!checkToken("equation", parser, Parser::nextEquation, x -> x, last, added)) return false; }
            else if(s.equals("%;%")) {if(!checkToken(";", parser, (data) -> new Token(TokenType.END_LINE), last, added)) return false; }
            else if(s.equals("%,%")) {if(!checkToken(",", parser, (data) -> new Token(TokenType.COMMA), last, added)) return false; }
            else if(s.equals("%(%")) {if(!checkToken("(", parser, (data) -> new Token(TokenType.OPEN), last, added)) return false; }
            else if(s.equals("%)%")) {if(!checkToken(")", parser, (data) -> new Token(TokenType.CLOSE), last, added)) return false; }
            else if(s.equals("%{%")) {if(!checkToken("{", parser, (data) -> new Token(TokenType.OPEN_SCOPE), last, added)) return false; }
            else if(s.equals("%}%")) {if(!checkToken("}", parser, (data) -> new Token(TokenType.CLOSE_SCOPE), last, added)) return false; }
            else {
                if(s.startsWith("^")) {
                    String[] def = Language.DEFINES.get(s.substring(s.substring(1).startsWith("+") ? 2 : 1));
                    if(s.substring(1).startsWith("+")) {
                        if(!check(def, parser, last, added)) {
                            return false;
                        }
                        while(true) {
                            SavedLocation location = parser.getSaveLocation();
                            boolean success = true;
                            try {
                                if(!check(def, parser, last, added)) success = false;
                            } catch(ParserException e) {
                                success = false;
                            }
                            if(!success) {
                                parser.loadLocation(location);
                                break;
                            }
                        }
                    }else {
                        if(!check(def, parser, last, added)) {
                            return false;
                        }
                    }
                }else if(!checkToken(s, parser, WordToken::new, last, added)) return false;
            }
        }
        tTokens.addAll(added);
        return true;
    }

    public static Token nextToken(DataParser parser) {
        return getToken(parser, parser.nextWord());
    }

    public static Token getToken(DataParser parser, String word) {
        if(Language.TYPES.get(word) != null) {
            Type type = Language.TYPES.get(word);
            if(type.isPointer()) {
                throw new ParserException("Parsing pointers is not implemented", parser);
            }
            return new TypeToken(type);
        }
        else if(Language.KEYWORDS.contains(word)) throw new ParserException("Keywords not allowed here", parser);
        else if(DataParser.operatorCharacters.matcher(word).matches()) return new OperatorToken(word);
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
        while(true) {
            if((word = parser.seekWord()) == null) break;
            boolean lateStart = false;
            if(Language.KEYWORDS.contains(word)) {
                tokens.add(new KeywordToken(word));
                lateStart = true;
            }
            ArrayList<String[]> validOptions = new ArrayList<>();
            for(String[] tok : Language.TOKENS) {
                parser.saveLocation();
                if(check(new String[] {tok[0]}, parser, false, new ArrayList<>())) {
                    validOptions.add(tok);
                }
                parser.loadLocation();
            }
            int index = 0;
            ArrayList<Token> tTokens = new ArrayList<>();
            boolean valid = false;
            for(String[] tok : validOptions) {
                tTokens.clear();
                parser.saveLocation();
                String[] used = tok;
                if(lateStart) {
                    parser.nextWord(); // Keyword is already added
                    used = new String[tok.length - 1];
                    System.arraycopy(tok, 1, used, 0, used.length);
                }
                if(check(used, parser, index + 1 == validOptions.size(), tTokens)) {
                    valid = true;
                    tokens.addAll(tTokens);
                    break;
                }
                parser.loadLocation();
                index++;
            }
            if(!valid) {
                throw new ParserException("Expression is invalid", parser);
            }
        }
        return tokens;

    }

    public static ArrayList<Token> parseObject(DataParser parser) {
        String name = parser.nextWord();
        ArrayList<Token> tokens = new ArrayList<>();

        String w = parser.nextWord();
        if(!w.equals("{") && !w.equals("extends")) {
            throw new ParserException("Expected '{' or 'extends' after name of object", parser);
        }
        String extendsFrom = null;
        if(w.equals("extends")) {
            extendsFrom = parser.nextWord();
            if(!parser.nextWord().equals("{")) {
                throw new ParserException("Expected '{' after name of extended object", parser);
            }
        }
        tokens.add(new ObjectToken(name, extendsFrom));

        while(true) {
            if(parser.seekWord().equals("}")) {
                tokens.add(new Token(TokenType.END_OBJECT));
                parser.nextWord();
                break;
            }
            String read = parser.nextWord();
            ArrayList<ParserFlag> flags = new ArrayList<>();
            while(true) {
                boolean found = false;
                for(ParserFlag flag : ParserFlag.values()) {
                    if(flag.isPublicFlag() && flag.name().toLowerCase().equals(read)) {
                        flags.add(flag);
                        read = parser.nextWord();
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    break;
                }
            }

            boolean readFunction = false;

            if(read.equals("field")) {
                String fieldType = parser.nextType();
                String fieldName = parser.nextWord();
                ArrayList<Token> ops = new ArrayList<>();
                String as = parser.nextWord();
                if(as.equals("=")) {
                    Token c;
                    while((c = nextToken(parser)).getType() != TokenType.END_LINE) {
                        ops.add(c);
                    }
                }else if(!as.equals(";")) {
                    throw new ParserException("Expected ';' or '=' after field declaration", parser);
                }
                ops.add(new Token(TokenType.END_LINE));
                tokens.add(new FieldToken(fieldType, fieldName, ops, flags));
            }else if(read.equals(name)) {
                ArrayList<ParameterDataType> functionParameters = parser.readParameters();
                String tmp = parser.nextWord();
                if(tmp.equals(";")) {
                    throw new ParserException("Constructors must have implementation", parser);
                }else if(!tmp.equals("{")) {
                    throw new ParserException("Expected '{' after arguments of constructor", parser);
                }

                tokens.add(new FunctionToken("void", "<init>", functionParameters, flags));
                readFunction = true;
            }else if(read.equals("function")) {
                String functionReturn = parser.nextType();
                String functionName = parser.nextWord();
                ArrayList<ParameterDataType> functionParameters = parser.readParameters();
                String tmp = parser.nextWord();
                if(tmp.equals(";")) {
                    flags.add(ParserFlag.NO_IMPLEMENTATION);
                }else if(!tmp.equals("{")) {
                    throw new ParserException("Expected '{' after arguments of function", parser);
                }

                tokens.add(new FunctionToken(functionReturn, functionName, functionParameters, flags));
                if(!flags.contains(ParserFlag.NO_IMPLEMENTATION)) readFunction = true;
            }else {
                throw new ParserException("Unexpected keyword '" + read + "'", parser);
            }

            if(readFunction) {
                tokens.addAll(readFunctionCode(parser));
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
            if((current.getType() == TokenType.END_LINE || current.getType() == TokenType.CLOSE) && open <= 0) {
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

    public static ArrayList<Token> readFunctionCode(DataParser parser) {
        ArrayList<Token> tokens = new ArrayList<>();
        int opens = 0;
        while(true) {
            if(parser.seekWord().equals("}") && opens == 0) {
                parser.nextWord();
                tokens.add(new Token(TokenType.END_FUNCTION));
                break;
            }
            String word = parser.nextWord();
            try {
                tokens.add(new NumberToken(getInteger(word)));
                continue;
            } catch(NumberFormatException ignored) {}

            if(DataParser.operatorCharacters.matcher(word).matches()) {
                tokens.add(new OperatorToken(word));
                continue;
            }
            if(word.equals(";")) {
                tokens.add(new Token(TokenType.END_LINE));
                continue;
            }else if(word.equals("(")) {
                tokens.add(new Token(TokenType.OPEN));
                continue;
            }else if(word.equals(")")) {
                tokens.add(new Token(TokenType.CLOSE));
                continue;
            }else if(word.equals(",")) {
                tokens.add(new Token(TokenType.COMMA));
                continue;
            }else if(word.equals(".")) {
                tokens.add(new Token(TokenType.ACCESSOR));
                continue;
            }else if(word.equals("\"")) {
                tokens.add(new StringToken(parser.nextStringStarted()));
                continue;
            }else if(word.equals("{")) {
                opens++;
                tokens.add(new Token(TokenType.OPEN));
                continue;
            }else if(word.equals("}")) {
                opens--;
                tokens.add(new Token(TokenType.CLOSE));
                continue;
            }
            tokens.add(new WordToken(word));
        }
        return tokens;
    }

}
