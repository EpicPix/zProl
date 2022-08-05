package ga.epicpix.zprol.parser;

import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.utils.SeekIterator;

import java.util.ArrayList;

public final class Parser {

    private final SeekIterator<LexerToken> lexerTokens;
    private Parser(SeekIterator<LexerToken> lexerTokens) {
        this.lexerTokens = lexerTokens;
    }

    public static ArrayList<Token> tokenize(SeekIterator<LexerToken> lexerTokens) {
        return new Parser(lexerTokens).tokenize();
    }

    public ArrayList<Token> tokenize() {
        var tokens = new ArrayList<Token>();
        while(skipWhitespace()) {
            var next = lexerTokens.next();
            if(next.name.equals("NamespaceKeyword")) {
                tokens.add(new NamedToken("Namespace", next, readNamespaceIdentifier(), wexpect("Semicolon")));
                continue;
            }else if(next.name.equals("UsingKeyword")) {
                tokens.add(new NamedToken("Using", next, readNamespaceIdentifier(), wexpect("Semicolon")));
                continue;
            }else if(next.name.equals("ClassKeyword")) {
                tokens.add(readClass(next));
                continue;
            }
            throw new TokenLocatedException("Expected 'namespace' or 'using' or 'class'", next);
        }
        return tokens;
    }

    public NamedToken readNamespaceIdentifier() {
        ArrayList<Token> tokens = new ArrayList<>();
        tokens.add(wexpect("Identifier"));
        LexerToken seperator;
        while((seperator = optional("AccessorOperator")) != null) {
            tokens.add(seperator);
            tokens.add(expect("Identifier"));
        }
        return new NamedToken("NamespaceIdentifier", tokens.toArray(new Token[0]));
    }

    public NamedToken readClass(LexerToken classKeyword) {
        ArrayList<Token> tokens = new ArrayList<>();
        tokens.add(classKeyword);
        tokens.add(wexpect("Identifier"));
        tokens.add(wexpect("OpenBrace"));

        tokens.add(wexpect("CloseBrace"));
        return new NamedToken("Class", tokens.toArray(new Token[0]));
    }


    // ---- Helper Methods ----

    public boolean skipWhitespace() {
        while(lexerTokens.hasNext() && lexerTokens.seek().name.equals("Whitespace")) {
            lexerTokens.next();
        }
        return lexerTokens.hasNext();
    }

    public LexerToken expect(String name) {
        if(!lexerTokens.hasNext()) {
            throw new TokenLocatedException("Expected '" + name + "' got end of file", lexerTokens.current());
        }
        var next = lexerTokens.next();
        if(next.name.equals(name)) {
            return next;
        }
        throw new TokenLocatedException("Expected '" + name + "', got '" + next.name + "'", next);
    }

    public LexerToken wexpect(String name) {
        skipWhitespace();
        return expect(name);
    }

    public LexerToken optional(String name) {
        if(!lexerTokens.hasNext()) {
            return null;
        }
        if(lexerTokens.seek().name.equals(name)) {
            return lexerTokens.next();
        }
        return null;
    }

}
