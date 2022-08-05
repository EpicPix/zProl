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
            }else if(next.name.equals("UsingKeyword")) {
                tokens.add(new NamedToken("Using", next, readNamespaceIdentifier(), wexpect("Semicolon")));
            }else if(next.name.equals("ClassKeyword")) {
                tokens.add(readClass(next));
            }else {
                lexerTokens.previous();
                tokens.add(readFieldOrMethod());
            }
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
        LexerToken close;
        skipWhitespace();
        while((close = optional("CloseBrace")) == null) {
            tokens.add(readFieldOrMethod());
            skipWhitespace();
        }
        tokens.add(close);
        return new NamedToken("Class", tokens.toArray(new Token[0]));
    }

    public NamedToken readMethod(ArrayList<Token> tokens) {
        throw new TokenLocatedException("Cannot read methods yet!", lexerTokens.current());
    }

    public NamedToken readField(ArrayList<Token> tokens) {
        tokens.add(wexpect("Semicolon"));
        return new NamedToken("Field", tokens.toArray(new Token[0]));
    }

    public NamedToken readFieldOrMethod() {
        ArrayList<Token> tokens = new ArrayList<>();
        if(skipWhitespace() && optional("ConstKeyword", tokens)) {
            tokens.add(wexpect("Identifier"));
            tokens.add(wexpect("AssignOperator"));
            tokens.add(wexpect("Semicolon"));
            return new NamedToken("Field", tokens.toArray(new Token[0]));
        }
        tokens.addAll(readFunctionModifiers());
        if(tokens.size() != 0) {
            return readMethod(tokens);
        }
        tokens.add(readType());
        tokens.add(wexpect("Identifier"));
        skipWhitespace();
        if(optional("OpenParen", tokens)) {
            return readMethod(tokens);
        }
        return readField(tokens);
    }

    public ArrayList<Token> readFunctionModifiers() {
        ArrayList<Token> tokens = new ArrayList<>();
        while(skipWhitespace()) {
            if(!optional("NativeKeyword", tokens)) {
                break;
            }
        }
        return tokens;
    }

    public NamedToken readType() {
        ArrayList<Token> tokens = new ArrayList<>();
        skipWhitespace();
        if(!optional("VoidKeyword", tokens)) {
            if(!optional("BoolKeyword", tokens)) {
                tokens.add(expect("Identifier"));
                while(optional("AccessorOperator", tokens)) {
                    tokens.add(expect("Identifier"));
                }
            }
            while(isNext("OpenBracket")) {
                tokens.add(new NamedToken("ArrayCharacters", expect("OpenBracket"), expect("CloseBracket")));
            }
        }
        return new NamedToken("Type", tokens);
    }

    // ---- Helper Methods ----

    public boolean skipWhitespace() {
        while(lexerTokens.hasNext() && (lexerTokens.seek().name.equals("Whitespace") || lexerTokens.seek().name.equals("Comment"))) {
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

    public LexerToken expect(String... names) {
        if(!lexerTokens.hasNext()) {
            throw new TokenLocatedException("Expected " + String.join(", ", names) + " got end of file", lexerTokens.current());
        }
        var next = lexerTokens.next();
        for(String name : names) {
            if(next.name.equals(name)) {
                return next;
            }
        }
        throw new TokenLocatedException("Expected " + String.join(", ", names) + " got '" + next.name + "'", next);
    }

    public LexerToken wexpect(String name) {
        skipWhitespace();
        return expect(name);
    }

    public LexerToken wexpect(String... names) {
        skipWhitespace();
        return expect(names);
    }

    public boolean isNext(String name) {
        if(!lexerTokens.hasNext()) {
            return false;
        }
        return lexerTokens.seek().name.equals(name);
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

    public boolean optional(String name, ArrayList<Token> tokens) {
        var opt = optional(name);
        if(opt == null) return false;
        tokens.add(opt);
        return true;
    }

}
