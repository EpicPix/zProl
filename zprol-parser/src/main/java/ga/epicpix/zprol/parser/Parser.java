package ga.epicpix.zprol.parser;

import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.parser.tokens.NamedToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tree.*;
import ga.epicpix.zprol.utils.SeekIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class Parser {

    private final SeekIterator<LexerToken> lexerTokens;
    private Parser(SeekIterator<LexerToken> lexerTokens) {
        this.lexerTokens = lexerTokens;
    }

    public static FileTree parse(SeekIterator<LexerToken> lexerTokens) {
        return new Parser(lexerTokens).parse();
    }

    public FileTree parse() {
        var declarations = new ArrayList<IDeclaration>();
        ParserState.create(lexerTokens);
        ParserState.pushLocation();
        FileTree file;
        try {
            while(skipWhitespace()) {
                if(isNext("NamespaceKeyword") || isNext("UsingKeyword")) {
                    ParserState.pushLocation();
                }
                var next = lexerTokens.next();
                switch(next.name) {
                    case "NamespaceKeyword" -> {
                        var identifier = readNamespaceIdentifier();
                        wexpect("Semicolon");
                        declarations.add(new NamespaceTree(identifier));
                    }
                    case "UsingKeyword" -> {
                        var identifier = readNamespaceIdentifier();
                        wexpect("Semicolon");
                        declarations.add(new UsingTree(identifier));
                    }
                    case "ClassKeyword" -> {
                        lexerTokens.previous();
                        declarations.add(readClass());
                    }
                    default -> {
                        lexerTokens.previous();
                        declarations.add(readFieldOrMethod());
                    }
                }
            }
        }finally {
            file = new FileTree(declarations);
            ParserState.delete();
        }
        return file;
    }

    public NamespaceIdentifierTree readNamespaceIdentifier() {
        ArrayList<LexerToken> tokens = new ArrayList<>();
        ParserState.pushLocation();
        tokens.add(wexpect("Identifier"));
        while(optional("AccessorOperator") != null) {
            tokens.add(expect("Identifier"));
        }
        return new NamespaceIdentifierTree(tokens.toArray(new LexerToken[0]));
    }

    public ClassTree readClass() {
        skipWhitespace();
        ParserState.pushLocation();
        expect("ClassKeyword");
        LexerToken name = wexpect("Identifier");
        wexpect("OpenBrace");
        ArrayList<IDeclaration> declarations = new ArrayList<>();
        while(skipWhitespace() && optional("CloseBrace") == null) {
            declarations.add(readFieldOrMethod());
        }
        return new ClassTree(name, declarations);
    }

    public FunctionTree readFunction(ModifiersTree mods, TypeTree type, LexerToken name) {
        wexpect("OpenParen");
        ParametersTree params = readParameterList("CloseParen");
        wexpect("CloseParen");
        skipWhitespace();
        if(optional("Semicolon") != null) {
            return new FunctionTree(mods, type, name, params, null);
        }
        CodeTree code = readCode();
        return new FunctionTree(mods, type, name, params, code);
    }

    public IDeclaration readFieldOrMethod() {
        ParserState.pushLocation();
        if(skipWhitespace() && optional("ConstKeyword") != null) {
            TypeTree type = readType();
            LexerToken name = wexpect("Identifier");
            wexpect("AssignOperator");
            IExpression expression = readExpression();
            wexpect("Semicolon");
            return new FieldTree(true, type, name, expression);
        }
        ModifiersTree mods = readFunctionModifiers();
        TypeTree type = readType();
        LexerToken name = wexpect("Identifier");
        skipWhitespace();
        if(mods.modifiers().length != 0 || isNext("OpenParen")) {
            return readFunction(mods, type, name);
        }
        wexpect("Semicolon");
        return new FieldTree(false, type, name, null);
    }

    public ModifiersTree readFunctionModifiers() {
        ArrayList<ModifierTree> mods = new ArrayList<>();
        ParserState.pushLocation();
        while(skipWhitespace()) {
            if(isNext("NativeKeyword")) {
                ParserState.pushLocation();
                expect("NativeKeyword");
                mods.add(new ModifierTree(ModifierTree.NATIVE));
                continue;
            }
            if(!isNext("NativeKeyword")) {
                break;
            }
        }
        return new ModifiersTree(mods.toArray(new ModifierTree[0]));
    }

    public TypeTree readType() {
        skipWhitespace();
        ParserState.pushLocation();
        int arrays = 0;
        if(isNext("VoidKeyword")) {
            return new TypeTree(expect("VoidKeyword"), 0);
        }else {
            LexerToken token;
            if((token = optional("BoolKeyword")) == null) {
                token = expect("Identifier");
            }
            while(isNext("OpenBracket")) {
                expect("OpenBracket");
                expect("CloseBracket");
                arrays++;
            }
            return new TypeTree(token, arrays);
        }
    }

    public ParametersTree readParameterList(String endToken) {
        skipWhitespace();
        ParserState.pushLocation();
        if(isNext(endToken)) {
            return new ParametersTree(new ParameterTree[0]);
        }
        ArrayList<ParameterTree> params = new ArrayList<>();
        params.add(readParameter());
        while(skipWhitespace() && !isNext(endToken)) {
            expect("CommaOperator");
            params.add(readParameter());
        }
        return new ParametersTree(params.toArray(new ParameterTree[0]));
    }

    public ParameterTree readParameter() {
        skipWhitespace();
        ParserState.pushLocation();
        TypeTree type = readType();
        LexerToken name = wexpect("Identifier");
        return new ParameterTree(type, name);
    }

    public CodeTree readCode() {
        skipWhitespace();
        ParserState.pushLocation();
        if(!isNext("OpenBrace")) {
            expect("LineCodeChars");
            IStatement statement = readStatement();
            return new CodeTree(new ArrayList<>(List.of(statement)));
        }
        expect("OpenBrace");
        ArrayList<IStatement> statements = new ArrayList<>();
        while(skipWhitespace() && optional("CloseBrace") == null) {
            statements.add(readStatement());
        }
        return new CodeTree(statements);
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
        throw new TokenLocatedException("TODO", lexerTokens.current());
//        ArrayList<Token> tokens = new ArrayList<>();
//        tokens.add(wexpect("ReturnKeyword"));
//        skipWhitespace();
//        if(!isNext("Semicolon")) {
//            tokens.add(readExpression());
//        }
//        tokens.add(wexpect("Semicolon"));
//        return new NamedToken("ReturnStatement", tokens.toArray(new Token[0]));
    }

    public NamedToken readWhileStatement() {
        throw new TokenLocatedException("TODO", lexerTokens.current());
//        ArrayList<Token> tokens = new ArrayList<>();
//        tokens.add(wexpect("WhileKeyword"));
//        tokens.add(wexpect("OpenParen"));
//        tokens.add(readExpression());
//        tokens.add(wexpect("CloseParen"));
//        tokens.add(readCode());
//        return new NamedToken("WhileStatement", tokens.toArray(new Token[0]));
    }

    public IfStatementTree readIfStatement() {
        skipWhitespace();
        ParserState.pushLocation();
        expect("IfKeyword");
        wexpect("OpenParen");
        IExpression expression = readExpression();
        wexpect("CloseParen");
        CodeTree code = readCode();
        skipWhitespace();
        ElseStatementTree elseStatement = null;
        if(isNext("ElseKeyword")) {
            elseStatement = readElseStatement();
        }
        return new IfStatementTree(expression, code, elseStatement);
    }

    public ElseStatementTree readElseStatement() {
        skipWhitespace();
        ParserState.pushLocation();
        expect("ElseKeyword");
        CodeTree code = readCode();
        return new ElseStatementTree(code);
    }

    public NamedToken readCreateAssignmentStatement(Token type) {
        throw new TokenLocatedException("TODO", lexerTokens.current());
//        ArrayList<Token> tokens = new ArrayList<>();
//        tokens.add(type);
//        tokens.add(wexpect("Identifier"));
//        tokens.add(wexpect("AssignOperator"));
//        tokens.add(readExpression());
//        tokens.add(wexpect("Semicolon"));
//        return new NamedToken("CreateAssignmentStatement", tokens.toArray(new Token[0]));
    }

    public IStatement readStatement() {
        skipWhitespace();
        ParserState.pushLocation();
        if(isNext("IfKeyword")) {
            return readIfStatement();
        }else throw new TokenLocatedException("TODO", lexerTokens.current());
//        if(isNext("WhileKeyword")) {
//            tokens.add(readWhileStatement());
//        }else if(isNext("BreakKeyword")) {
//            tokens.add(readBreakStatement());
//        }else if(isNext("ContinueKeyword")) {
//            tokens.add(readContinueStatement());
//        }else if(isNext("ReturnKeyword")) {
//            tokens.add(readReturnStatement());
//        }else if(isNext("Identifier")) {
//            ArrayList<Token> statement = new ArrayList<>();
//            int start = lexerTokens.currentIndex();
//            try {
//                statement.add(readType());
//            }catch(TokenLocatedException e) {
//                lexerTokens.setIndex(start);
//                statement.add(readAccessor());
//            }
//            skipWhitespace();
//            if(!isNext("Identifier")) {
//                lexerTokens.setIndex(start);
//                statement.clear();
//                statement.add(readAccessor());
//                skipWhitespace();
//                if(optional("AssignOperator", statement)) {
//                    statement.add(readExpression());
//                    statement.add(wexpect("Semicolon"));
//                    tokens.add(new NamedToken("AssignmentStatement", statement.toArray(new Token[0])));
//                }else {
//                    statement.add(wexpect("Semicolon"));
//                    tokens.add(new NamedToken("AccessorStatement", statement.toArray(new Token[0])));
//                }
//            }else {
//                tokens.add(readCreateAssignmentStatement(statement.get(0)));
//            }
//        }else {
//            tokens.add(readCreateAssignmentStatement(readType()));
//        }
//        return new NamedToken("Statement", tokens.toArray(new Token[0]));
    }

    public IExpression readExpression() {
        throw new TokenLocatedException("TODO", lexerTokens.current());
//        return new NamedToken("Expression", readInclusiveAndExpression());
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
        throw new TokenLocatedException("TODO", lexerTokens.current());
//        ArrayList<Token> tokens = new ArrayList<>();
//        skipWhitespace();
//        if(optional("OpenParen", tokens)) {
//            var start = lexerTokens.currentIndex();
//            ArrayList<Token> cast = new ArrayList<>();
//            try {
//                cast.add(readType());
//                optional("HardCastIndicatorOperator", cast);
//                cast.add(wexpect("CloseParen"));
//            }catch(TokenLocatedException e) {
//                lexerTokens.setIndex(start);
//                var expr = readExpression();
//                tokens.add(wexpect("CloseParen"));
//                return expr;
//            }
//            tokens.addAll(cast);
//            return new NamedToken("CastExpression", new NamedToken("CastOperator", tokens.toArray(new Token[0])), readPostExpression());
//        }
//        skipWhitespace();
//        if(isNext("Identifier")) {
//            return readAccessor();
//        }
//        return expect("String", "NullKeyword", "Integer", "TrueKeyword", "FalseKeyword");
    }

    public NamedToken readAccessor() {
        throw new TokenLocatedException("TODO", lexerTokens.current());
//        ArrayList<Token> tokens = new ArrayList<>();
//        tokens.add(readFunctionInvocationAccessor());
//        while(skipWhitespace() && (isNext("OpenBracket") || isNext("AccessorOperator"))) {
//            if(isNext("OpenBracket")) {
//                tokens.add(new NamedToken("AccessorElement", new NamedToken("ArrayAccessor", expect("OpenBracket"), readExpression(), wexpect("CloseBracket"))));
//            }else {
//                tokens.add(new NamedToken("AccessorElement", expect("AccessorOperator"), readFunctionInvocationAccessor()));
//            }
//        }
//        return new NamedToken("Accessor", tokens.toArray(new Token[0]));
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
        throw new TokenLocatedException("TODO", lexerTokens.current());
//        ArrayList<Token> tokens = new ArrayList<>();
//        skipWhitespace();
//        if(isNext(endToken)) {
//            return;
//        }
//        tokens.add(readExpression());
//        while(skipWhitespace() && !isNext(endToken)) {
//            tokens.add(expect("CommaOperator"));
//            tokens.add(readExpression());
//        }
//        output.add(new NamedToken("ArgumentList", tokens.toArray(new Token[0])));
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

    // ---- AST to 'dot' Converter ---

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
