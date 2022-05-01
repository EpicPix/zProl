package ga.epicpix.zprol.parser;

import ga.epicpix.zprol.exceptions.NotImplementedException;
import ga.epicpix.zprol.parser.tokens.*;
import ga.epicpix.zprol.zld.Language;
import ga.epicpix.zprol.parser.DataParser.SavedLocation;
import ga.epicpix.zprol.exceptions.ParserException;
import ga.epicpix.zprol.zld.LanguageToken;
import ga.epicpix.zprol.zld.LanguageTokenFragment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class Parser {

    public static String getLanguageDefinition(LanguageToken f, String t) {
        StringBuilder builder = new StringBuilder(t + " ");
        var args = f.args();
        for(int i = 1; i<args.length; i++) {
            builder.append(args[i].getDebugName()).append(" ");
        }
        return builder.toString().trim();
    }

    public static String getLanguageDefinition(LanguageToken f) {
        StringBuilder builder = new StringBuilder();
        var args = f.args();
        for(LanguageTokenFragment arg : args) {
            builder.append(arg.getDebugName()).append(" ");
        }
        return builder.toString().trim();
    }

    public static boolean check(ArrayList<Token> tTokens, DataParser parser, LanguageToken token) {
        ArrayList<Token> added = new ArrayList<>();
        for(var fragment : token.args()) {
            var read = fragment.getTokenReader().apply(parser);
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
                LanguageToken langToken = null;
                for (LanguageToken tok : validOptions) {
                    parser.saveLocation();
                    ArrayList<Token> tTokens = new ArrayList<>();
                    if (check(tTokens, parser, tok)) {
                        langToken = tok;
                        tokens.add(new NamedToken(tok.name(), tTokens.toArray(new Token[0])));
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

    public static String generateAst(ArrayList<Token> tokens) {
        StringBuilder builder = new StringBuilder();
        builder.append("digraph {\n");
        AtomicInteger current = new AtomicInteger();
        int root = current.getAndIncrement();
        builder.append("  token").append(root).append("[label=\"<root>\"]\n");
        for(var t : tokens) {
            int num = writeTokenAst(t, builder, current);
            builder.append("  token").append(root).append(" -> token").append(num).append("\n");
        }
        builder.append("}");
        return builder.toString();
    }

    private static int writeTokenAst(Token token, StringBuilder builder, AtomicInteger current) {
        int index = current.getAndIncrement();
        if (token instanceof NamedToken named) {
            builder.append("  token").append(index).append("[label=\"").append(token.getType().name().toLowerCase()).append(" (").append(named.name).append(")\"]\n");
            for(var t : named.tokens) {
                int num = writeTokenAst(t, builder, current);
                builder.append("  token").append(index).append(" -> token").append(num).append("\n");
            }
        }else if (token instanceof WordHolder word) {
            builder.append("  token").append(index).append("[label=\"").append(token.getType().name().toLowerCase()).append(" \\\"").append(word.getWord()).append("\\\"\"]\n");
        }else if (token instanceof OperatorToken operator) {
            builder.append("  token").append(index).append("[label=\"").append("operator \\\"").append(operator.operator).append("\\\"\"]\n");
        }else if (token instanceof NumberToken number) {
            builder.append("  token").append(index).append("[label=\"").append("number ").append(number.number).append("\"]\n");
        }else if(token instanceof EquationToken equation) {
            builder.append("  token").append(index).append("[label=\"").append(token.getType().name().toLowerCase()).append("\"]\n");
            for(var t : equation.tokens) {
                int num = writeTokenAst(t, builder, current);
                builder.append("  token").append(index).append(" -> token").append(num).append("\n");
            }
        }else if(token.getType() == TokenType.OPEN) {
            builder.append("  token").append(index).append("[label=\"(\"]\n");
        }else if(token.getType() == TokenType.CLOSE) {
            builder.append("  token").append(index).append("[label=\")\"]\n");
        }else if(token.getType() == TokenType.OPEN_SCOPE) {
            builder.append("  token").append(index).append("[label=\"{\"]\n");
        }else if(token.getType() == TokenType.CLOSE_SCOPE) {
            builder.append("  token").append(index).append("[label=\"}\"]\n");
        }else if(token.getType() == TokenType.END_LINE) {
            builder.append("  token").append(index).append("[label=\";\"]\n");
        }else if(token.getType() == TokenType.ACCESSOR) {
            builder.append("  token").append(index).append("[label=\".\"]\n");
        }else if(token.getType() == TokenType.COMMA) {
            builder.append("  token").append(index).append("[label=\",\"]\n");
        }else {
            builder.append("  token").append(index).append("[label=\"").append(token.getType().name().toLowerCase()).append("\"]\n");
        }
        return index;
    }

}
