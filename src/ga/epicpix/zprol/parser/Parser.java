package ga.epicpix.zprol.parser;

import ga.epicpix.zprol.zld.Language;
import ga.epicpix.zprol.parser.DataParser.SavedLocation;
import ga.epicpix.zprol.exceptions.ParserException;
import ga.epicpix.zprol.parser.tokens.EquationToken;
import ga.epicpix.zprol.parser.tokens.KeywordToken;
import ga.epicpix.zprol.parser.tokens.NumberToken;
import ga.epicpix.zprol.parser.tokens.OperatorToken;
import ga.epicpix.zprol.parser.tokens.ParsedToken;
import ga.epicpix.zprol.parser.tokens.StringToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tokens.TokenType;
import ga.epicpix.zprol.parser.tokens.WordToken;
import ga.epicpix.zprol.zld.LanguageToken;
import ga.epicpix.zprol.zld.LanguageTokenFragment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;

public class Parser {

    private static String getLanguageDefinition(LanguageToken f, String t) {
        StringBuilder builder = new StringBuilder(t + " ");
        var args = f.args();
        for(int i = 1; i<args.length; i++) {
            builder.append(args[i].getDebugName()).append(" ");
        }
        return builder.toString().trim();
    }

    public static boolean check(ArrayList<Token> tTokens, DataParser parser, int offset, LanguageTokenFragment... t) {
        ArrayList<Token> added = new ArrayList<>();
        for(int i = offset; i<t.length; i++) {
            var read = t[i].getTokenReader().apply(parser);
            if (read == null) {
                return false;
            }
            Collections.addAll(added, read);
        }
        tTokens.addAll(added);
        return true;
    }

    public static Token nextToken(DataParser parser) {
        return getToken(parser, parser.nextWord());
    }

    public static Token getToken(DataParser parser, String word) {
        if(Language.KEYWORDS.get(word) != null) throw new ParserException("Keywords not allowed here", parser);
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
            boolean skip = false;
            if(word.equals("{")) {
                parser.nextWord();
                tokens.add(new Token(TokenType.OPEN_SCOPE));
            }else if(word.equals("}")) {
                parser.nextWord();
                tokens.add(new Token(TokenType.CLOSE_SCOPE));
            }else if(!skip) {
                ArrayList<LanguageToken> validOptions = new ArrayList<>();
                for(LanguageToken tok : Language.TOKENS) {
                    parser.saveLocation();
                    if(tok.args()[0].getTokenReader().apply(parser) != null) {
                        validOptions.add(tok);
                    }
                    parser.loadLocation();
                }
                ArrayList<Token> preAdded = new ArrayList<>();
                int offset = 0;
                var k = Language.KEYWORDS.get(word);
                if(k != null) {
                    preAdded.add(new KeywordToken(k));
                    offset = 1;
                }
                LanguageToken langToken = null;
                for (LanguageToken tok : validOptions) {
                    parser.saveLocation();
                    if(offset != 0) {
                        parser.nextWord();
                    }
                    ArrayList<Token> tTokens = new ArrayList<>(preAdded);
                    if (check(tTokens, parser, offset, tok.args())) {
                        langToken = tok;
                        tokens.add(new ParsedToken(tok.name(), tTokens));
                        parser.discardLocation();
                        break;
                    }
                    parser.loadLocation();
                }
                if(langToken == null) {
                    String[] expressions = new String[validOptions.size()];
                    String l = parser.nextWord();
                    for(int j = 0; j<expressions.length; j++) expressions[j] = "Expected " + getLanguageDefinition(validOptions.get(j), l);
                    throw new ParserException(expressions.length != 0 ? String.join("\n", expressions) : "Invalid expression", parser);
                }
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
            if(!(current.getType() == TokenType.WORD || current.getType() == TokenType.OPERATOR || current.getType() == TokenType.NUMBER || current.getType() == TokenType.STRING || current.getType() == TokenType.ACCESSOR) && open <= 0) {
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
