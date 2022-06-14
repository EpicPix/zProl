package ga.epicpix.zprol.parser;

import ga.epicpix.zprol.parser.exceptions.ParserException;
import ga.epicpix.zprol.parser.lexer.LanguageLexerToken;
import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.parser.tokens.*;
import ga.epicpix.zprol.utils.SeekIterator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class Parser {

    public static boolean check(ArrayList<String> tTokens, DataParser parser, LanguageLexerToken token) {
        for(var fragment : token.args()) {
            var read = fragment.apply(parser);
            if (read == null) {
                return false;
            }
            tTokens.add(read);
        }
        return true;
    }

    public static boolean check(ArrayList<Token> tTokens, SeekIterator<LexerToken> tokens, LanguageToken token) {
        ArrayList<Token> added = new ArrayList<>();
        for(var fragment : token.args()) {
            var read = fragment.apply(tokens);
            if (read == null) {
                return false;
            }
            Collections.addAll(added, read);
        }
        tTokens.addAll(added);
        return true;
    }

    public static ArrayList<Token> tokenize(File file) throws IOException {
        var lexed = lex(file.getName(), Files.readAllLines(file.toPath()).toArray(new String[0]));
        return tokenize(new SeekIterator<>(lexed));
    }

    public static ArrayList<LexerToken> lex(String filename, String[] lines) {
        DataParser parser = new DataParser(filename, lines);
        ArrayList<LexerToken> tokens = new ArrayList<>();
        next: while(parser.hasNext()) {
            for(var lexerToken : LanguageLexerToken.LEXER_TOKENS) {
                ArrayList<String> x = new ArrayList<>();
                var saveStart = parser.saveLocation();
                var start = parser.getLocation();
                if(check(x, parser, lexerToken)) {
                    tokens.add(new LexerToken(lexerToken.name(), x.size() != 0 ? x.get(0) : "", start, parser.getLocation(), parser));
                    continue next;
                }
                parser.loadLocation(saveStart);
            }

            throw new ParserException("Unknown lexer token", parser, parser.getLocation());
        }
        return tokens;
    }

    public static ArrayList<Token> tokenize(SeekIterator<LexerToken> lexerTokens) {
        ArrayList<Token> tokens = new ArrayList<>();

        while(lexerTokens.hasNext()) {
            LanguageToken langToken = null;
            for (var tok : LanguageToken.TOKENS) {
                var loc = lexerTokens.currentIndex();
                ArrayList<Token> tTokens = new ArrayList<>();
                if (check(tTokens, lexerTokens, tok)) {
                    langToken = tok;
                    if(tTokens.size() != 0) {
                        tokens.add(new NamedToken(tok.name(), tTokens.get(0).startLocation, tTokens.get(tTokens.size() - 1).endLocation, lexerTokens.current().parser, tTokens.toArray(new Token[0])));
                    }else {
                        tokens.add(new NamedToken(tok.name(), lexerTokens.get(loc).startLocation, lexerTokens.get(loc).endLocation, lexerTokens.get(loc).parser));
                    }
                    break;
                }
                lexerTokens.setIndex(loc);
            }
            if(langToken == null) {
                throw new ParserException("Failed to parse", lexerTokens.current().parser, lexerTokens.current().startLocation);
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
        }else if (token instanceof LexerToken lexer) {
            builder.append("  token").append(index).append("[shape=box,color=\"#007FFF\",label=\"").append("(").append(lexer.name).append(")\"]\n");
            int indexI = current.getAndIncrement();
            builder.append("  token").append(indexI).append("[shape=box,color=\"#00FFFF\",label=\"").append(lexer.data.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\\\\n")).append("\"]\n");
            builder.append("  token").append(index).append(" -> token").append(indexI).append("\n");
        }
        return index;
    }

}
