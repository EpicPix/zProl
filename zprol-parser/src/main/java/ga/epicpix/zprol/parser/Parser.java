package ga.epicpix.zprol.parser;

import ga.epicpix.zprol.parser.exceptions.ParserException;
import ga.epicpix.zprol.parser.tokens.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class Parser {

    public static boolean check(ArrayList<Token> tTokens, DataParser parser, LanguageToken token) {
        ArrayList<Token> added = new ArrayList<>();
        for(var fragment : token.args()) {
            var read = fragment.apply(parser);
            if (read == null) {
                return false;
            }
            Collections.addAll(added, read);
        }
        tTokens.addAll(added);
        return true;
    }

    public static ArrayList<Token> tokenize(File file) throws IOException {
        return tokenize(file.getName(), Files.readAllLines(file.toPath()).toArray(new String[0]));
    }

    public static ArrayList<Token> tokenize(String fileName, String[] lines) {
        DataParser parser = new DataParser(fileName, lines);

        ArrayList<Token> tokens = new ArrayList<>();

        while(parser.seekWord() != null) {
            LanguageToken langToken = null;
            for (var tok : LanguageToken.TOKENS) {
                var loc = parser.saveLocation();
                ArrayList<Token> tTokens = new ArrayList<>();
                var startLocation = parser.getLocation();
                if (check(tTokens, parser, tok)) {
                    langToken = tok;
                    tokens.add(new NamedToken(tok.name(), startLocation, parser.getLocation(), parser, tTokens.toArray(new Token[0])));
                    break;
                }
                parser.loadLocation(loc);
            }
            if(langToken == null) {
                throw new ParserException("Failed to parse", parser);
            }
        }
        return tokens;

    }

    public static String generateParseTree(ArrayList<Token> tokens) {
        StringBuilder builder = new StringBuilder();
        builder.append("digraph {\n");
        AtomicInteger current = new AtomicInteger();
        int root = current.getAndIncrement();
        builder.append("  token").append(root).append("[shape=box,color=\"#007FFF\",label=\"<root>\"]\n");
        for(var t : tokens) {
            int num = writeTokenParseTree(t, builder, current);
            builder.append("  token").append(root).append(" -> token").append(num).append("\n");
        }
        builder.append("}");
        return builder.toString();
    }

    private static int writeTokenParseTree(Token token, StringBuilder builder, AtomicInteger current) {
        int index = current.getAndIncrement();
        if (token instanceof NamedToken named) {
            builder.append("  token").append(index).append("[shape=box,color=\"#FF7F00\",label=\"").append("(").append(named.name).append(")\"]\n");
            for(var t : named.tokens) {
                int num = writeTokenParseTree(t, builder, current);
                builder.append("  token").append(index).append(" -> token").append(num).append("\n");
            }
        }else if (token instanceof WordHolder word) {
            builder.append("  token").append(index).append("[shape=box,color=\"#7F7F7F\",label=\"").append("\\\"").append(word.getWord().replace("\\", "\\\\").replace("\"", "\\\\\\\"")).append("\\\"\"]\n");
        }else if(token.getType() == TokenType.OPEN) {
            builder.append("  token").append(index).append("[shape=box,color=\"#FF3F00\",label=\"(\"]\n");
        }else if(token.getType() == TokenType.CLOSE) {
            builder.append("  token").append(index).append("[shape=box,color=\"#FF3F00\",label=\")\"]\n");
        }else if(token.getType() == TokenType.OPEN_SCOPE) {
            builder.append("  token").append(index).append("[shape=box,color=\"#FF3F00\",label=\"{\"]\n");
        }else if(token.getType() == TokenType.CLOSE_SCOPE) {
            builder.append("  token").append(index).append("[shape=box,color=\"#FF3F00\",label=\"}\"]\n");
        }else if(token.getType() == TokenType.END_LINE) {
            builder.append("  token").append(index).append("[shape=box,color=\"#FF3F00\",label=\";\"]\n");
        }else if(token.getType() == TokenType.ACCESSOR) {
            builder.append("  token").append(index).append("[shape=box,color=\"#FF3F00\",label=\".\"]\n");
        }else if(token.getType() == TokenType.COMMA) {
            builder.append("  token").append(index).append("[shape=box,color=\"#FF3F00\",label=\",\"]\n");
        }else {
            builder.append("  token").append(index).append("[shape=box,color=\"#FF00FF\",label=\"").append(token.getType().name().toLowerCase()).append("\"]\n");
        }
        return index;
    }

}
