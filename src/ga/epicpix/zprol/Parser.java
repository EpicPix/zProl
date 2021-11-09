package ga.epicpix.zprol;

import ga.epicpix.zprol.exceptions.ParserException;
import ga.epicpix.zprol.tokens.FieldToken;
import ga.epicpix.zprol.tokens.FunctionToken;
import ga.epicpix.zprol.tokens.KeywordToken;
import ga.epicpix.zprol.tokens.LongWordToken;
import ga.epicpix.zprol.tokens.NumberToken;
import ga.epicpix.zprol.tokens.ObjectToken;
import ga.epicpix.zprol.tokens.OperatorToken;
import ga.epicpix.zprol.tokens.StringToken;
import ga.epicpix.zprol.tokens.StructureToken;
import ga.epicpix.zprol.tokens.Token;
import ga.epicpix.zprol.tokens.TokenType;
import ga.epicpix.zprol.tokens.WordToken;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

public class Parser {

    public static ArrayList<Token> tokenize(String fileName) throws IOException {
        File file = new File(fileName);
        if(!file.exists()) {
            throw new FileNotFoundException(fileName);
        }

        DataParser parser = new DataParser(new File(fileName).getName(), Files.readAllLines(file.toPath()).toArray(new String[0]));

        ArrayList<Token> tokens = new ArrayList<>();

        ArrayList<ParserFlag> flags = new ArrayList<>();
        String word;
        while((word = parser.nextWord()) != null) {
            if(Language.KEYWORDS.contains(word)) {
                tokens.add(new KeywordToken(word));
                ArrayList<String[]> tok = Language.TOKENS.get(word);
                if(tok != null) {
                    boolean valid = false;
                    ArrayList<Token> tTokens = new ArrayList<>();
                    int i = 0;
                    for(String[] t : tok) {
                        tTokens.clear();
                        parser.saveLocation();
                        boolean v = true;
                        for(String s : t) {
                            if(s.equals("@lword@")) {
                                String w = parser.nextLongWord();
                                if(w == null) {
                                    v = false;
                                    if(i + 1 == tok.size()) {
                                        throw new ParserException("Expected lword", parser);
                                    } else {
                                        break;
                                    }
                                }
                                tTokens.add(new LongWordToken(w));
                            }else if(s.equals("@word@")) {
                                String w = parser.nextWord();
                                if(w == null) {
                                    v = false;
                                    if(i + 1 == tok.size()) {
                                        throw new ParserException("Expected word", parser);
                                    } else {
                                        break;
                                    }
                                }
                                tTokens.add(new WordToken(w));
                            }else if(s.equals("@type@")) {
                                String w = parser.nextType();
                                if(w == null) {
                                    v = false;
                                    if(i + 1 == tok.size()) {
                                        throw new ParserException("Expected type", parser);
                                    } else {
                                        break;
                                    }
                                }
                                tTokens.add(new WordToken(w));
                            }else if(s.equals("@line@")) {
                                String w = parser.nextWord();
                                if(!";".equals(w)) {
                                    v = false;
                                    if(i + 1 == tok.size()) {
                                        throw new ParserException("Expected ';'", parser);
                                    } else {
                                        break;
                                    }
                                }
                                tTokens.add(new Token(TokenType.END_LINE));
                            }else {
                                String w = parser.nextWord();
                                if(!s.equals(w)) {
                                    v = false;
                                    if(i + 1 == tok.size()) {
                                        throw new ParserException("Expected " + s, parser);
                                    } else {
                                        break;
                                    }
                                }
                                tTokens.add(new WordToken(w));
                            }
                        }
                        if(v) {
                            valid = true;
                            tokens.addAll(tTokens);
                            break;
                        }
                        parser.loadLocation();
                        i++;
                    }

                    if(!valid) {
                        throw new ParserException("Expression is invalid", parser);
                    }
                }
            } else if(word.equals("structure")) {
                tokens.add(parseStructure(parser));
            } else if(word.equals("object")) {
                tokens.addAll(parseObject(parser));
            } else if(word.equals("function")) {
                String functionReturn = parser.nextType();
                String functionName = parser.nextWord();
                ArrayList<ParameterDataType> functionParameters = parser.readParameters();
                String tmp = parser.nextWord();
                if(tmp.equals(";")) {
                    throw new ParserException("Missing function implementation", parser);
                }else if(!tmp.equals("{")) {
                    throw new ParserException("Expected '{' after arguments of function", parser);
                }

                tokens.add(new FunctionToken(functionReturn, functionName, functionParameters, new ArrayList<>()));
                tokens.addAll(readFunctionCode(parser));
            } else if(word.equals("field")) {
                String type = parser.nextType();
                String name = parser.nextWord();
                ArrayList<Token> ops = new ArrayList<>();
                String as = parser.nextWord();
                if(as.equals("=")) {
                    Token c;
                    while((c = getToken(parser)).getType() != TokenType.END_LINE) {
                        ops.add(c);
                    }
                }else if(!as.equals(";")) {
                    throw new ParserException("Expected ';' or '=' after field declaration", parser);
                }
                ops.add(new Token(TokenType.END_LINE));
                tokens.add(new FieldToken(type, name, ops, new ArrayList<>(flags)));
            } else if(ParserFlag.getFlag(word) != null) {
                ParserFlag f = ParserFlag.getFlag(word);
                if(!flags.contains(f)) {
                    flags.add(f);
                    continue;
                }else {
                    throw new ParserException("Redefined flag: " + f.name().toLowerCase(), parser);
                }
            } else {
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
                    tokens.add(new Token(TokenType.OPEN_SCOPE));
                    continue;
                }else if(word.equals("}")) {
                    tokens.add(new Token(TokenType.CLOSE_SCOPE));
                    continue;
                }else {
                    tokens.add(new WordToken(word));
                }
            }
            flags.clear();
        }
        return tokens;

    }

    public static StructureToken parseStructure(DataParser parser) {
        String name = parser.nextWord();
        if(!parser.nextWord().equals("{")) {
            throw new ParserException("Missing '{' after structure name", parser);
        }
        ArrayList<StructureType> types = new ArrayList<>();
        while(true) {
            if(parser.seekWord().equals("}")) {
                parser.nextWord();
                break;
            }
            String sType = parser.nextType();
            String sName = parser.nextWord();
            types.add(new StructureType(sType, sName));
            if(!(parser.nextWord()).equals(";")) {
                throw new ParserException("Missing ';' after structure field definition", parser);
            }
        }
        return new StructureToken(name, types);
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
                    while((c = getToken(parser)).getType() != TokenType.END_LINE) {
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

    public static Token getToken(DataParser parser) {
        String word = parser.nextWord();
        try {
            return new NumberToken(getInteger(word));
        } catch(NumberFormatException ignored) {}

        if(DataParser.operatorCharacters.matcher(word).matches()) {
            return new OperatorToken(word);
        }
        if(word.equals(";")) {
            return new Token(TokenType.END_LINE);
        }else if(word.equals("(")) {
            return new Token(TokenType.OPEN);
        }else if(word.equals(")")) {
            return new Token(TokenType.CLOSE);
        }else if(word.equals(",")) {
            return new Token(TokenType.COMMA);
        }else if(word.equals(".")) {
            return new Token(TokenType.ACCESSOR);
        }else if(word.equals("\"")) {
            return new StringToken(parser.nextStringStarted());
        }else if(word.equals("{")) {
            return new Token(TokenType.OPEN);
        }else if(word.equals("}")) {
            return new Token(TokenType.CLOSE);
        }
        return new WordToken(word);
    }

}
