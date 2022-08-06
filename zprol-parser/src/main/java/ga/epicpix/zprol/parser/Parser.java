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

    public static ArrayList<Token> parse(SeekIterator<LexerToken> lexerTokens) {
        return new Parser(lexerTokens).parse();
    }

    public ArrayList<Token> parse() {
        var tokens = new ArrayList<Token>();
        while(skipWhitespace()) {
            var next = lexerTokens.next();
            switch(next.name) {
                case "NamespaceKeyword" -> tokens.add(new NamedToken("Namespace", next, readNamespaceIdentifier(), wexpect("Semicolon")));
                case "UsingKeyword" -> tokens.add(new NamedToken("Using", next, readNamespaceIdentifier(), wexpect("Semicolon")));
                case "ClassKeyword" -> tokens.add(readClass(next));
                default -> {
                    lexerTokens.previous();
                    tokens.add(readFieldOrMethod());
                }
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
        tokens.add(wexpect("OpenParen"));
        readParameterList("CloseParen", tokens);
        wexpect("CloseParen");
        skipWhitespace();
        if(optional("Semicolon", tokens)) {
            return new NamedToken("Function", tokens.toArray(new Token[0]));
        }
        tokens.add(readCode());
        return new NamedToken("Function", tokens.toArray(new Token[0]));
    }

    public NamedToken readField(ArrayList<Token> tokens) {
        tokens.add(wexpect("Semicolon"));
        return new NamedToken("Field", tokens.toArray(new Token[0]));
    }

    public NamedToken readFieldOrMethod() {
        ArrayList<Token> tokens = new ArrayList<>();
        if(skipWhitespace() && optional("ConstKeyword", tokens)) {
            tokens.add(readType());
            tokens.add(wexpect("Identifier"));
            tokens.add(wexpect("AssignOperator"));
            tokens.add(readExpression());
            tokens.add(wexpect("Semicolon"));
            return new NamedToken("Field", tokens.toArray(new Token[0]));
        }
        tokens.addAll(readFunctionModifiers());
        if(tokens.size() != 0) {
            tokens.add(readType());
            tokens.add(wexpect("Identifier"));
            return readMethod(tokens);
        }
        tokens.add(readType());
        tokens.add(wexpect("Identifier"));
        skipWhitespace();
        if(isNext("OpenParen")) {
            return readMethod(tokens);
        }
        return readField(tokens);
    }

    public ArrayList<Token> readFunctionModifiers() {
        ArrayList<Token> tokens = new ArrayList<>();
        while(skipWhitespace()) {
            if(isNext("NativeKeyword")) {
                tokens.add(new NamedToken("FunctionModifier", expect("NativeKeyword")));
                continue;
            }
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

    public void readParameterList(String endToken, ArrayList<Token> output) {
        ArrayList<Token> tokens = new ArrayList<>();
        skipWhitespace();
        if(isNext(endToken)) {
            return;
        }
        tokens.add(readParameter());
        while(skipWhitespace() && !isNext(endToken)) {
            tokens.add(expect("CommaOperator"));
            tokens.add(readParameter());
        }
        output.add(new NamedToken("ParameterList", tokens.toArray(new Token[0])));
    }

    public NamedToken readParameter() {
        return new NamedToken("Parameter", readType(), wexpect("Identifier"));
    }

    public NamedToken readCode() {
        ArrayList<Token> tokens = new ArrayList<>();
        skipWhitespace();
        if(!isNext("OpenBrace")) {
            tokens.add(expect("LineCodeChars"));
            tokens.add(readStatement());
            return new NamedToken("Code", tokens.toArray(new Token[0]));
        }
        tokens.add(expect("OpenBrace"));
        while(skipWhitespace() && !isNext("CloseBrace")) {
            tokens.add(readStatement());
        }
        tokens.add(wexpect("CloseBrace"));
        return new NamedToken("Code", tokens.toArray(new Token[0]));
    }

    public NamedToken readBreakStatement() {
        ArrayList<Token> tokens = new ArrayList<>();
        tokens.add(wexpect("BreakKeyword"));
        tokens.add(wexpect("Semicolon"));
        return new NamedToken("BreakStatement", tokens.toArray(new Token[0]));
    }

    public NamedToken readContinueStatement() {
        ArrayList<Token> tokens = new ArrayList<>();
        tokens.add(wexpect("ContinueKeyword"));
        tokens.add(wexpect("Semicolon"));
        return new NamedToken("ContinueStatement", tokens.toArray(new Token[0]));
    }

    public NamedToken readReturnStatement() {
        ArrayList<Token> tokens = new ArrayList<>();
        tokens.add(wexpect("ReturnKeyword"));
        skipWhitespace();
        if(!isNext("Semicolon")) {
            tokens.add(readExpression());
        }
        tokens.add(wexpect("Semicolon"));
        return new NamedToken("ReturnStatement", tokens.toArray(new Token[0]));
    }

    public NamedToken readWhileStatement() {
        ArrayList<Token> tokens = new ArrayList<>();
        tokens.add(wexpect("WhileKeyword"));
        tokens.add(wexpect("OpenParen"));
        tokens.add(readExpression());
        tokens.add(wexpect("CloseParen"));
        tokens.add(readCode());
        return new NamedToken("WhileStatement", tokens.toArray(new Token[0]));
    }

    public NamedToken readIfStatement() {
        ArrayList<Token> tokens = new ArrayList<>();
        tokens.add(wexpect("IfKeyword"));
        tokens.add(wexpect("OpenParen"));
        tokens.add(readExpression());
        tokens.add(wexpect("CloseParen"));
        tokens.add(readCode());
        skipWhitespace();
        if(isNext("ElseKeyword")) {
            tokens.add(readElseStatement());
        }
        return new NamedToken("IfStatement", tokens.toArray(new Token[0]));
    }

    public NamedToken readElseStatement() {
        ArrayList<Token> tokens = new ArrayList<>();
        tokens.add(wexpect("ElseKeyword"));
        tokens.add(readCode());
        return new NamedToken("ElseStatement", tokens.toArray(new Token[0]));
    }

    public NamedToken readCreateAssignmentStatement(Token type) {
        ArrayList<Token> tokens = new ArrayList<>();
        tokens.add(type);
        tokens.add(wexpect("Identifier"));
        tokens.add(wexpect("AssignOperator"));
        tokens.add(readExpression());
        tokens.add(wexpect("Semicolon"));
        return new NamedToken("CreateAssignmentStatement", tokens.toArray(new Token[0]));
    }

    public NamedToken readStatement() {
        ArrayList<Token> tokens = new ArrayList<>();
        skipWhitespace();
        if(isNext("IfKeyword")) {
            tokens.add(readIfStatement());
        }else if(isNext("WhileKeyword")) {
            tokens.add(readWhileStatement());
        }else if(isNext("BreakKeyword")) {
            tokens.add(readBreakStatement());
        }else if(isNext("ContinueKeyword")) {
            tokens.add(readContinueStatement());
        }else if(isNext("ReturnKeyword")) {
            tokens.add(readReturnStatement());
        }else if(isNext("Identifier")) {
            ArrayList<Token> statement = new ArrayList<>();
            int start = lexerTokens.currentIndex();
            try {
                statement.add(readType());
            }catch(TokenLocatedException e) {
                lexerTokens.setIndex(start);
                statement.add(readAccessor());
            }
            skipWhitespace();
            if(!isNext("Identifier")) {
                lexerTokens.setIndex(start);
                statement.clear();
                statement.add(readAccessor());
                skipWhitespace();
                if(optional("AssignOperator", statement)) {
                    statement.add(readExpression());
                    statement.add(wexpect("Semicolon"));
                    tokens.add(new NamedToken("AssignmentStatement", statement.toArray(new Token[0])));
                }else {
                    statement.add(wexpect("Semicolon"));
                    tokens.add(new NamedToken("AccessorStatement", statement.toArray(new Token[0])));
                }
            }else {
                tokens.add(readCreateAssignmentStatement(statement.get(0)));
            }
        }else {
            tokens.add(readCreateAssignmentStatement(readType()));
        }
        return new NamedToken("Statement", tokens.toArray(new Token[0]));
    }

    public Token readExpression() {
        return new NamedToken("Expression", readInclusiveAndExpression());
    }

    public Token readInclusiveAndExpression() {
        skipWhitespace();
        ArrayList<Token> tokens = new ArrayList<>();
        tokens.add(readInclusiveOrExpression());
        if(skipWhitespace() && optional("AndOperator", tokens)) {
            tokens.add(readInclusiveAndExpression());
        }
        if(tokens.size() == 1) return tokens.get(0);
        return new NamedToken("InclusiveAndExpression", tokens.toArray(new Token[0]));
    }

    public Token readInclusiveOrExpression() {
        skipWhitespace();
        ArrayList<Token> tokens = new ArrayList<>();
        tokens.add(readShiftExpression());
        if(skipWhitespace() && optional("InclusiveOrOperator", tokens)) {
            tokens.add(readInclusiveOrExpression());
        }
        if(tokens.size() == 1) return tokens.get(0);
        return new NamedToken("InclusiveOrExpression", tokens.toArray(new Token[0]));
    }

    public Token readShiftExpression() {
        skipWhitespace();
        ArrayList<Token> tokens = new ArrayList<>();
        tokens.add(readEqualsExpression());
        if(skipWhitespace() && optional(tokens, "ShiftLeftOperator", "ShiftRightOperator")) {
            tokens.add(readShiftExpression());
        }
        if(tokens.size() == 1) return tokens.get(0);
        return new NamedToken("ShiftExpression", tokens.toArray(new Token[0]));
    }


    public Token readEqualsExpression() {
        skipWhitespace();
        ArrayList<Token> tokens = new ArrayList<>();
        tokens.add(readCompareExpression());
        if(skipWhitespace() && optional(tokens, "EqualOperator", "NotEqualOperator")) {
            tokens.add(readEqualsExpression());
        }
        if(tokens.size() == 1) return tokens.get(0);
        return new NamedToken("EqualsExpression", tokens.toArray(new Token[0]));
    }


    public Token readCompareExpression() {
        skipWhitespace();
        ArrayList<Token> tokens = new ArrayList<>();
        tokens.add(readAdditiveExpression());
        if(skipWhitespace() && optional(tokens, "LessEqualThanOperator", "LessThanOperator", "GreaterEqualThanOperator", "GreaterThanOperator")) {
            tokens.add(readCompareExpression());
        }
        if(tokens.size() == 1) return tokens.get(0);
        return new NamedToken("CompareExpression", tokens.toArray(new Token[0]));
    }


    public Token readAdditiveExpression() {
        skipWhitespace();
        ArrayList<Token> tokens = new ArrayList<>();
        tokens.add(readMultiplicativeExpression());
        if(skipWhitespace() && optional(tokens, "AddOperator", "SubtractOperator")) {
            tokens.add(readAdditiveExpression());
        }
        if(tokens.size() == 1) return tokens.get(0);
        return new NamedToken("AdditiveExpression", tokens.toArray(new Token[0]));
    }


    public Token readMultiplicativeExpression() {
        skipWhitespace();
        ArrayList<Token> tokens = new ArrayList<>();
        tokens.add(readPostExpression());
        if(skipWhitespace() && optional(tokens, "MultiplyOperator", "DivideOperator", "ModuloOperator")) {
            tokens.add(readMultiplicativeExpression());
        }
        if(tokens.size() == 1) return tokens.get(0);
        return new NamedToken("MultiplicativeExpression", tokens.toArray(new Token[0]));
    }


    public Token readPostExpression() {
        ArrayList<Token> tokens = new ArrayList<>();
        skipWhitespace();
        if(optional("OpenParen", tokens)) {
            var start = lexerTokens.currentIndex();
            ArrayList<Token> cast = new ArrayList<>();
            try {
                cast.add(readType());
                optional("HardCastIndicatorOperator", cast);
                cast.add(wexpect("CloseParen"));
            }catch(TokenLocatedException e) {
                lexerTokens.setIndex(start);
                var expr = readExpression();
                tokens.add(wexpect("CloseParen"));
                return expr;
            }
            tokens.addAll(cast);
            return new NamedToken("CastExpression", new NamedToken("CastOperator", tokens.toArray(new Token[0])), readPostExpression());
        }
        skipWhitespace();
        if(isNext("Identifier")) {
            return readAccessor();
        }
        return expect("String", "NullKeyword", "Integer", "TrueKeyword", "FalseKeyword");
    }

    public NamedToken readAccessor() {
        ArrayList<Token> tokens = new ArrayList<>();
        tokens.add(readFunctionInvocationAccessor());
        while(skipWhitespace() && (isNext("OpenBracket") || isNext("AccessorOperator"))) {
            if(isNext("OpenBracket")) {
                tokens.add(new NamedToken("AccessorElement", new NamedToken("ArrayAccessor", expect("OpenBracket"), readExpression(), wexpect("CloseBracket"))));
            }else {
                tokens.add(new NamedToken("AccessorElement", expect("AccessorOperator"), readFunctionInvocationAccessor()));
            }
        }
        return new NamedToken("Accessor", tokens.toArray(new Token[0]));
    }

    public Token readFunctionInvocationAccessor() {
        var ident = wexpect("Identifier");
        skipWhitespace();
        ArrayList<Token> tokens = new ArrayList<>();
        if(optional("OpenParen", tokens)) {
            readArgumentList("CloseParen", tokens);
            tokens.add(wexpect("CloseParen"));
            return new NamedToken("FunctionInvocationAccessor", ident, new NamedToken("FunctionInvocation", tokens.toArray(new Token[0])));
        }
        return ident;
    }

    public void readArgumentList(String endToken, ArrayList<Token> output) {
        ArrayList<Token> tokens = new ArrayList<>();
        skipWhitespace();
        if(isNext(endToken)) {
            return;
        }
        tokens.add(readExpression());
        while(skipWhitespace() && !isNext(endToken)) {
            tokens.add(expect("CommaOperator"));
            tokens.add(readExpression());
        }
        output.add(new NamedToken("ArgumentList", tokens.toArray(new Token[0])));
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

    public boolean optional(ArrayList<Token> tokens, String... names) {
        for(String name : names) {
            var opt = optional(name);
            if(opt == null) continue;
            tokens.add(opt);
            return true;
        }
        return false;
    }

}
