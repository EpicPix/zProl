package ga.epicpix.zprol.parser;

import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.parser.tokens.Token;
import ga.epicpix.zprol.parser.tree.*;
import ga.epicpix.zprol.utils.SeekIterator;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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

    public BreakStatementTree readBreakStatement() {
        skipWhitespace();
        ParserState.pushLocation();
        expect("BreakKeyword");
        wexpect("Semicolon");
        return new BreakStatementTree();
    }

    public ContinueStatementTree readContinueStatement() {
        skipWhitespace();
        ParserState.pushLocation();
        expect("ContinueKeyword");
        wexpect("Semicolon");
        return new ContinueStatementTree();
    }

    public ReturnStatementTree readReturnStatement() {
        skipWhitespace();
        ParserState.pushLocation();
        expect("ReturnKeyword");
        skipWhitespace();
        if(optional("Semicolon") != null) {
            return new ReturnStatementTree(null);
        }
        IExpression expression = readExpression();
        wexpect("Semicolon");
        return new ReturnStatementTree(expression);
    }

    public WhileStatementTree readWhileStatement() {
        skipWhitespace();
        ParserState.pushLocation();
        wexpect("WhileKeyword");
        wexpect("OpenParen");
        IExpression expression = readExpression();
        wexpect("CloseParen");
        CodeTree code = readCode();
        return new WhileStatementTree(expression, code);
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

    public CreateAssignmentStatementTree readCreateAssignmentStatement(TypeTree type) {
        LexerToken name = wexpect("Identifier");
        wexpect("AssignOperator");
        IExpression expression = readExpression();
        wexpect("Semicolon");
        return new CreateAssignmentStatementTree(type, name, expression);
    }

    public IStatement readStatement() {
        skipWhitespace();
        ParserState.pushLocation();
        if(isNext("IfKeyword")) {
            return readIfStatement();
        }else if(isNext("WhileKeyword")) {
            return readWhileStatement();
        }else if(isNext("BreakKeyword")) {
            return readBreakStatement();
        }else if(isNext("ContinueKeyword")) {
            return readContinueStatement();
        }else if(isNext("ReturnKeyword")) {
            return readReturnStatement();
        }else if(isNext("Identifier")) {
            int start = lexerTokens.currentIndex();
            TypeTree type = null;
            try {
                type = readType();
            }catch(TokenLocatedException e) {
                ParserState.popLocation(); // pop readType location
                lexerTokens.setIndex(start);
                readAccessor();
            }
            skipWhitespace();
            if(!isNext("Identifier")) {
                lexerTokens.setIndex(start);
                AccessorTree accessor = readAccessor();
                skipWhitespace();
                if(optional("AssignOperator") != null) {
                    IExpression expression = readExpression();
                    wexpect("Semicolon");
                    return new AssignmentStatementTree(accessor, expression);
                }else {
                    wexpect("Semicolon");
                    return new AccessorStatementTree(accessor);
                }
            }else {
                if(type == null) throw new TokenLocatedException("Expected a type", lexerTokens.get(start));
                return readCreateAssignmentStatement(type);
            }
        }else {
            return readCreateAssignmentStatement(readType());
        }
    }

    public IExpression readExpression() {
        return readInclusiveAndExpression();
    }

    public IExpression readInclusiveAndExpression() {
        skipWhitespace();
        ParserState.pushLocation();
        IExpression expression = readInclusiveOrExpression();
        LexerToken operator;
        if(skipWhitespace() && (operator = optional("AndOperator")) != null) {
            return new OperatorExpressionTree(expression, operator, readInclusiveAndExpression());
        }else {
            ParserState.popLocation();
            return expression;
        }
    }

    public IExpression readInclusiveOrExpression() {
        skipWhitespace();
        ParserState.pushLocation();
        IExpression expression = readShiftExpression();
        LexerToken operator;
        if(skipWhitespace() && (operator = optional("InclusiveOrOperator")) != null) {
            return new OperatorExpressionTree(expression, operator, readInclusiveOrExpression());
        }else {
            ParserState.popLocation();
            return expression;
        }
    }

    public IExpression readShiftExpression() {
        skipWhitespace();
        ParserState.pushLocation();
        IExpression expression = readEqualsExpression();
        LexerToken operator;
        if(skipWhitespace() && (operator = optional("ShiftLeftOperator", "ShiftRightOperator")) != null) {
            return new OperatorExpressionTree(expression, operator, readShiftExpression());
        }else {
            ParserState.popLocation();
            return expression;
        }
    }


    public IExpression readEqualsExpression() {
        skipWhitespace();
        ParserState.pushLocation();
        IExpression expression = readCompareExpression();
        LexerToken operator;
        if(skipWhitespace() && (operator = optional("EqualOperator", "NotEqualOperator")) != null) {
            return new OperatorExpressionTree(expression, operator, readEqualsExpression());
        }else {
            ParserState.popLocation();
            return expression;
        }
    }


    public IExpression readCompareExpression() {
        skipWhitespace();
        ParserState.pushLocation();
        IExpression expression = readAdditiveExpression();
        LexerToken operator;
        if(skipWhitespace() && (operator = optional("LessEqualThanOperator", "LessThanOperator", "GreaterEqualThanOperator", "GreaterThanOperator")) != null) {
            return new OperatorExpressionTree(expression, operator, readCompareExpression());
        }else {
            ParserState.popLocation();
            return expression;
        }
    }


    public IExpression readAdditiveExpression() {
        skipWhitespace();
        ParserState.pushLocation();
        IExpression expression = readMultiplicativeExpression();
        LexerToken operator;
        if(skipWhitespace() && (operator = optional("AddOperator", "SubtractOperator")) != null) {
            return new OperatorExpressionTree(expression, operator, readAdditiveExpression());
        }else {
            ParserState.popLocation();
            return expression;
        }
    }


    public IExpression readMultiplicativeExpression() {
        skipWhitespace();
        ParserState.pushLocation();
        IExpression expression = readPostExpression();
        LexerToken operator;
        if(skipWhitespace() && (operator = optional("MultiplyOperator", "DivideOperator", "ModuloOperator")) != null) {
            return new OperatorExpressionTree(expression, operator, readMultiplicativeExpression());
        }else {
            ParserState.popLocation();
            return expression;
        }
    }


    public IExpression readPostExpression() {
        skipWhitespace();
        ParserState.pushLocation();
        if(optional("OpenParen") != null) {
            var start = lexerTokens.currentIndex();
            TypeTree type;
            boolean hardCast;
            try {
                type = readType();
                hardCast = optional("HardCastIndicatorOperator") != null;
                wexpect("CloseParen");
            }catch(TokenLocatedException e) { // possibly broke at readType(), might break the stack
                lexerTokens.setIndex(start);
                var expr = readExpression();
                wexpect("CloseParen");
                return expr;
            }
            return new CastTree(type, hardCast, readPostExpression());
        }
        skipWhitespace();
        if(isNext("Identifier")) {
            return readAccessor();
        }
        if(isNext("String")) {
            return new LiteralTree(LiteralType.STRING, expect("String").data);
        }else if(optional("NullKeyword") != null) {
            return new LiteralTree(LiteralType.NULL, null);
        }else if(isNext("Integer")) {
            return new LiteralTree(LiteralType.INTEGER, ParserUtils.getInteger(expect("Integer")));
        }else if(optional("TrueKeyword") != null) {
            return new LiteralTree(LiteralType.BOOLEAN, true);
        }else if(optional("FalseKeyword") != null) {
            return new LiteralTree(LiteralType.BOOLEAN, false);
        }
        throw new TokenLocatedException("Expected an expression got " + lexerTokens.current().name, lexerTokens.current());
    }

    public AccessorTree readAccessor() {
        ParserState.pushLocation();
        ArrayList<IAccessorElement> elements = new ArrayList<>();
        elements.add(readFunctionInvocationAccessor());
        while(skipWhitespace() && (isNext("OpenBracket") || isNext("AccessorOperator"))) {
            if(isNext("OpenBracket")) {
                ParserState.pushLocation();
                expect("OpenBracket");
                IExpression expression = readExpression();
                expect("CloseBracket");
                elements.add(new ArrayAccessTree(expression));
            }else {
                expect("AccessorOperator");
                elements.add(readFunctionInvocationAccessor());
            }
        }
        return new AccessorTree(elements.toArray(new IAccessorElement[0]));
    }

    public IAccessorElement readFunctionInvocationAccessor() {
        skipWhitespace();
        ParserState.pushLocation();
        var ident = expect("Identifier");
        if(optional("OpenParen") != null) {
            ArgumentsTree args = readArgumentList("CloseParen");
            wexpect("CloseParen");
            return new FunctionCallTree(ident, args);
        }
        return new FieldAccessTree(ident);
    }

    public ArgumentsTree readArgumentList(String endToken) {
        skipWhitespace();
        ParserState.pushLocation();
        if(isNext(endToken)) {
            return new ArgumentsTree(new IExpression[0]);
        }
        ArrayList<IExpression> arguments = new ArrayList<>();
        arguments.add(readExpression());
        while(skipWhitespace() && !isNext(endToken)) {
            expect("CommaOperator");
            arguments.add(readExpression());
        }
        return new ArgumentsTree(arguments.toArray(new IExpression[0]));
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

    public LexerToken optional(String... names) {
        for(String name : names) {
            var opt = optional(name);
            if(opt == null) continue;
            return opt;
        }
        return null;
    }

}
