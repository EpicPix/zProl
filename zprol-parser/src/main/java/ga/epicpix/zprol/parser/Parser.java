package ga.epicpix.zprol.parser;

import ga.epicpix.zprol.parser.exceptions.ParserException;
import ga.epicpix.zprol.parser.lexer.LanguageLexerToken;
import ga.epicpix.zprol.parser.tokens.*;
import ga.epicpix.zprol.parser.zld.ZldParser;
import ga.epicpix.zprol.utils.SeekIterator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class Parser {

    public static String check(DataParser parser, LanguageLexerToken token) {
        StringBuilder builder = new StringBuilder();
        nextFragment: for(var fragment : token.args()) {
            if(fragment.multi()) {
                fLoop: do {
                    var loc = parser.saveLocation();
                    int cChar = parser.nextChar();
                    if(cChar == -1) continue nextFragment;
                    for (int c : fragment.characters()) {
                        if (c == cChar) {
                            if (!fragment.negate()) {
                                builder.appendCodePoint(cChar);
                                continue fLoop;
                            }else {
                                parser.loadLocation(loc);
                                continue nextFragment;
                            }
                        }
                    }
                    if(fragment.negate()) {
                        builder.appendCodePoint(cChar);
                        continue;
                    }

                    parser.loadLocation(loc);
                    continue nextFragment;
                }while(true);
            }else {
                var loc = parser.saveLocation();
                int cChar = parser.nextChar();
                if(cChar == -1) continue;
                for(int c : fragment.characters()) {
                    if(c==cChar) {
                        if(!fragment.negate()) {
                            builder.appendCodePoint(cChar);
                        }else {
                            parser.loadLocation(loc);
                        }
                        continue nextFragment;
                    }
                }
                if(fragment.negate()) {
                    builder.appendCodePoint(cChar);
                    continue;
                }
            }
            return null;
        }
        return builder.toString();
    }

    public static boolean checkClean(DataParser parser, LanguageLexerToken token) {
        nextFragment: for(var fragment : token.args()) {
            if(fragment.multi()) {
                fLoop: do {
                    var loc = parser.saveLocation();
                    int cChar = parser.nextChar();
                    if(cChar == -1) continue nextFragment;
                    for (int c : fragment.characters()) {
                        if (c == cChar) {
                            if (!fragment.negate()) {
                                continue fLoop;
                            }else {
                                continue nextFragment;
                            }
                        }
                    }

                    if(fragment.negate()) {
                        continue;
                    }

                    parser.loadLocation(loc);
                    continue nextFragment;
                }while(true);
            }else {
                int cChar = parser.nextChar();
                if(cChar == -1) continue;
                for(int c : fragment.characters()) {
                    if(c==cChar) {
                        continue nextFragment;
                    }
                }
                if(fragment.negate()) {
                    continue;
                }
            }
            return false;
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
            ArrayList<LanguageLexerToken> possibilities = new ArrayList<>();

            var loc = parser.saveLocation();

            int checkChar = parser.nextChar();
            nextToken: for(var lexerToken : LanguageLexerToken.LEXER_TOKENS) {
                var frag = lexerToken.args()[0];
                for(int c : frag.characters()) {
                    if(c==checkChar) {
                        if(!frag.negate()) {
                            possibilities.add(lexerToken);
                            continue nextToken;
                        }
                    }
                }
                if(frag.negate()) {
                    possibilities.add(lexerToken);
                }
            }

            parser.loadLocation(loc);

            if(possibilities.size() != 0) {
                LexerToken res = null;
                DataParser.SavedLocation endLocation = null;
                for(int i = 0, pSize = possibilities.size(); i < pSize; i++) {
                    LanguageLexerToken possibility = possibilities.get(i);
                    var saveStart = parser.saveLocation();
                    var start = parser.getLocation();
                    if(possibility.clean()) {
                        boolean result = checkClean(parser, possibility);
                        if(result) {
                            tokens.add(new LexerToken(possibility.name(), "", possibility, start, parser.getLocation(), parser));
                            continue next;
                        }
                    }else {
                        String result;
                        if ((result = check(parser, possibility)) != null) {
                            if(res == null || result.length() > res.data.length()) {
                                endLocation = parser.saveLocation();
                                res = new LexerToken(possibility.name(), result, possibility, start, parser.getLocation(), parser);
                            }
                        }
                    }
                    parser.loadLocation(saveStart);
                }
                if(res != null) {
                    parser.loadLocation(endLocation);
                    tokens.add(res);
                    continue;
                }
            }

            throw new ParserException("Unknown lexer token", parser, parser.getLocation());
        }
        return tokens;
    }

    public static ArrayList<Token> tokenize(SeekIterator<LexerToken> lexerTokens) {
        ArrayList<Token> tTokens = new ArrayList<>();
        if(check(tTokens, lexerTokens, ZldParser.root)) {
            if(lexerTokens.hasNext()) {
                throw new ParserException("Not everything has been read from the file", lexerTokens.current().parser);
            }
            return tTokens;
        }
        throw new ParserException("Could not parse", lexerTokens.current().parser);
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
