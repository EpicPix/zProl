package ga.epicpix.zprol.parser;

import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.parser.tokens.TokenType;
import ga.epicpix.zprol.parser.tree.*;
import ga.epicpix.zprol.utils.SeekIterator;

import java.util.ArrayList;
import java.util.List;

import static ga.epicpix.zprol.parser.tokens.TokenType.*;

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
        int _start = curr();
        while(skipWhitespace()) {
            int start = curr();
            switch(lexerTokens.seek().type) {
                case NamespaceKeyword -> {
                    lexerTokens.next();
                    var identifier = readNamespaceIdentifier(start);
                    wexpect(Semicolon);
                    declarations.add(new NamespaceTree(start, curr(), identifier));
                }
                case UsingKeyword -> {
                    lexerTokens.next();
                    var identifier = readNamespaceIdentifier(start);
                    wexpect(Semicolon);
                    declarations.add(new UsingTree(start, curr(), identifier));
                }
                case ClassKeyword -> declarations.add(readClass(start));
                default -> declarations.add(readFieldOrMethod());
            }
        }
        return new FileTree(_start, curr(), declarations);
    }

    public NamespaceIdentifierTree readNamespaceIdentifier(int start) {
        ArrayList<LexerToken> tokens = new ArrayList<>();
        tokens.add(wexpect(Identifier));
        while(optional(AccessorOperator) != null) {
            tokens.add(expect(Identifier));
        }
        return new NamespaceIdentifierTree(start, curr(), tokens.toArray(new LexerToken[0]));
    }

    public ClassTree readClass(int start) {
        skipWhitespace();
        expect(ClassKeyword);
        LexerToken name = wexpect(Identifier);
        wexpect(OpenBrace);
        ArrayList<IDeclaration> declarations = new ArrayList<>();
        while(skipWhitespace() && optional(CloseBrace) == null) {
            declarations.add(readFieldOrMethod());
        }
        return new ClassTree(start, curr(), name, declarations);
    }

    public FunctionTree readFunction(int start, ModifiersTree mods, TypeTree type, LexerToken name) {
        wexpect(OpenParen);
        ParametersTree params = readParameterList(CloseParen);
        wexpect(CloseParen);
        skipWhitespace();
        if(optional(Semicolon) != null) {
            return new FunctionTree(start, curr(), mods, type, name, params, null);
        }
        CodeTree code = readCode();
        return new FunctionTree(start, curr(), mods, type, name, params, code);
    }

    public IDeclaration readFieldOrMethod() {
        int start = curr();
        if(skipWhitespace() && optional(ConstKeyword) != null) {
            TypeTree type = readType();
            LexerToken name = wexpect(Identifier);
            wexpect(AssignOperator);
            IExpression expression = readExpression();
            wexpect(Semicolon);
            return new FieldTree(start, curr(), true, type, name, expression);
        }
        ModifiersTree mods = readFunctionModifiers();
        TypeTree type = readType();
        LexerToken name = wexpect(Identifier);
        skipWhitespace();
        if(mods.modifiers().length != 0 || isNext(OpenParen)) {
            return readFunction(start, mods, type, name);
        }
        wexpect(Semicolon);
        return new FieldTree(start, curr(), false, type, name, null);
    }

    public ModifiersTree readFunctionModifiers() {
        ArrayList<ModifierTree> mods = new ArrayList<>();
        int start = curr();
        while(skipWhitespace()) {
            if(isNext(NativeKeyword)) {
                int localStart = curr();
                expect(NativeKeyword);
                mods.add(new ModifierTree(localStart, curr(), ModifierTree.NATIVE));
                continue;
            }
            if(!isNext(NativeKeyword)) {
                break;
            }
        }
        return new ModifiersTree(start, curr(), mods.toArray(new ModifierTree[0]));
    }

    public TypeTree readType() {
        skipWhitespace();
        int start = curr();
        int arrays = 0;
        if(isNext(VoidKeyword)) {
            return new TypeTree(start, curr(), expect(VoidKeyword), 0);
        }else {
            LexerToken token;
            if((token = optional(BoolKeyword)) == null) {
                token = expect(Identifier);
            }
            while(isNext(OpenBracket)) {
                expect(OpenBracket);
                expect(CloseBracket);
                arrays++;
            }
            return new TypeTree(start, curr(), token, arrays);
        }
    }

    public ParametersTree readParameterList(TokenType endToken) {
        skipWhitespace();
        int start = curr();
        if(isNext(endToken)) {
            return new ParametersTree(start, curr(), new ParameterTree[0]);
        }
        ArrayList<ParameterTree> params = new ArrayList<>();
        params.add(readParameter());
        while(skipWhitespace() && !isNext(endToken)) {
            expect(CommaOperator);
            params.add(readParameter());
        }
        return new ParametersTree(start, curr(), params.toArray(new ParameterTree[0]));
    }

    public ParameterTree readParameter() {
        skipWhitespace();
        int start = curr();
        TypeTree type = readType();
        LexerToken name = wexpect(Identifier);
        return new ParameterTree(start, curr(), type, name);
    }

    public CodeTree readCode() {
        skipWhitespace();
        int start = curr();
        if(!isNext(OpenBrace)) {
            expect(LineCodeChars);
            IStatement statement = readStatement();
            return new CodeTree(start, curr(), new ArrayList<>(List.of(statement)));
        }
        expect(OpenBrace);
        ArrayList<IStatement> statements = new ArrayList<>();
        while(skipWhitespace() && optional(CloseBrace) == null) {
            statements.add(readStatement());
        }
        return new CodeTree(start, curr(), statements);
    }

    public BreakStatementTree readBreakStatement() {
        skipWhitespace();
        int start = curr();
        expect(BreakKeyword);
        wexpect(Semicolon);
        return new BreakStatementTree(start, curr());
    }

    public ContinueStatementTree readContinueStatement() {
        skipWhitespace();
        int start = curr();
        expect(ContinueKeyword);
        wexpect(Semicolon);
        return new ContinueStatementTree(start, curr());
    }

    public ReturnStatementTree readReturnStatement() {
        skipWhitespace();
        int start = curr();
        expect(ReturnKeyword);
        skipWhitespace();
        if(optional(Semicolon) != null) {
            return new ReturnStatementTree(start, curr(), null);
        }
        IExpression expression = readExpression();
        wexpect(Semicolon);
        return new ReturnStatementTree(start, curr(), expression);
    }

    public WhileStatementTree readWhileStatement() {
        skipWhitespace();
        int start = curr();
        wexpect(WhileKeyword);
        wexpect(OpenParen);
        IExpression expression = readExpression();
        wexpect(CloseParen);
        CodeTree code = readCode();
        return new WhileStatementTree(start, curr(), expression, code);
    }

    public IfStatementTree readIfStatement() {
        skipWhitespace();
        int start = curr();
        expect(IfKeyword);
        wexpect(OpenParen);
        IExpression expression = readExpression();
        wexpect(CloseParen);
        CodeTree code = readCode();
        skipWhitespace();
        ElseStatementTree elseStatement = null;
        if(isNext(ElseKeyword)) {
            elseStatement = readElseStatement();
        }
        return new IfStatementTree(start, curr(), expression, code, elseStatement);
    }

    public ElseStatementTree readElseStatement() {
        skipWhitespace();
        int start = curr();
        expect(ElseKeyword);
        CodeTree code = readCode();
        return new ElseStatementTree(start, curr(), code);
    }

    public CreateAssignmentStatementTree readCreateAssignmentStatement(int start, TypeTree type) {
        LexerToken name = wexpect(Identifier);
        wexpect(AssignOperator);
        IExpression expression = readExpression();
        wexpect(Semicolon);
        return new CreateAssignmentStatementTree(start, curr(), type, name, expression);
    }

    public IStatement readStatement() {
        skipWhitespace();
        int start = curr();
        if(isNext(IfKeyword)) {
            return readIfStatement();
        }else if(isNext(WhileKeyword)) {
            return readWhileStatement();
        }else if(isNext(BreakKeyword)) {
            return readBreakStatement();
        }else if(isNext(ContinueKeyword)) {
            return readContinueStatement();
        }else if(isNext(ReturnKeyword)) {
            return readReturnStatement();
        }else if(isNext(Identifier)) {
            TypeTree type = null;
            try {
                type = readType();
            }catch(TokenLocatedException e) {
                lexerTokens.setIndex(start);
                readAccessor();
            }
            skipWhitespace();
            if(!isNext(Identifier)) {
                lexerTokens.setIndex(start);
                AccessorTree accessor = readAccessor();
                skipWhitespace();
                if(optional(AssignOperator) != null) {
                    IExpression expression = readExpression();
                    wexpect(Semicolon);
                    return new AssignmentStatementTree(start, curr(), accessor, expression);
                }else {
                    wexpect(Semicolon);
                    return new AccessorStatementTree(start, curr(), accessor);
                }
            }else {
                if(type == null) throw new TokenLocatedException("Expected a type", lexerTokens.get(start));
                return readCreateAssignmentStatement(start, type);
            }
        }else {
            return readCreateAssignmentStatement(start, readType());
        }
    }

    public IExpression readExpression() {
        return readInclusiveAndExpression();
    }

    public IExpression readInclusiveAndExpression() {
        skipWhitespace();
        int start = curr();
        IExpression expression = readInclusiveOrExpression();
        LexerToken operator;
        if(skipWhitespace() && (operator = optional(AndOperator)) != null) {
            return new OperatorExpressionTree(start, curr(), expression, operator, readInclusiveAndExpression());
        }else {
            return expression;
        }
    }

    public IExpression readInclusiveOrExpression() {
        skipWhitespace();
        int start = curr();
        IExpression expression = readShiftExpression();
        LexerToken operator;
        if(skipWhitespace() && (operator = optional(InclusiveOrOperator)) != null) {
            return new OperatorExpressionTree(start, curr(), expression, operator, readInclusiveOrExpression());
        }else {
            return expression;
        }
    }

    public IExpression readShiftExpression() {
        skipWhitespace();
        int start = curr();
        IExpression expression = readEqualsExpression();
        LexerToken operator;
        if(skipWhitespace() && (operator = optional(ShiftLeftOperator, ShiftRightOperator)) != null) {
            return new OperatorExpressionTree(start, curr(), expression, operator, readShiftExpression());
        }else {
            return expression;
        }
    }


    public IExpression readEqualsExpression() {
        skipWhitespace();
        int start = curr();
        IExpression expression = readCompareExpression();
        LexerToken operator;
        if(skipWhitespace() && (operator = optional(EqualOperator, NotEqualOperator)) != null) {
            return new OperatorExpressionTree(start, curr(), expression, operator, readEqualsExpression());
        }else {
            return expression;
        }
    }


    public IExpression readCompareExpression() {
        skipWhitespace();
        int start = curr();
        IExpression expression = readAdditiveExpression();
        LexerToken operator;
        if(skipWhitespace() && (operator = optional(LessEqualThanOperator, LessThanOperator, GreaterEqualThanOperator, GreaterThanOperator)) != null) {
            return new OperatorExpressionTree(start, curr(), expression, operator, readCompareExpression());
        }else {
            return expression;
        }
    }


    public IExpression readAdditiveExpression() {
        skipWhitespace();
        int start = curr();
        IExpression expression = readMultiplicativeExpression();
        LexerToken operator;
        if(skipWhitespace() && (operator = optional(AddOperator, SubtractOperator)) != null) {
            return new OperatorExpressionTree(start, curr(), expression, operator, readAdditiveExpression());
        }else {
            return expression;
        }
    }


    public IExpression readMultiplicativeExpression() {
        skipWhitespace();
        int start = curr();
        IExpression expression = readPostExpression();
        LexerToken operator;
        if(skipWhitespace() && (operator = optional(MultiplyOperator, DivideOperator, ModuloOperator)) != null) {
            return new OperatorExpressionTree(start, curr(), expression, operator, readMultiplicativeExpression());
        }else {
            return expression;
        }
    }


    public IExpression readPostExpression() {
        skipWhitespace();
        int start = curr();
        if(optional(OpenParen) != null) {
            int _start = curr();
            TypeTree type;
            boolean hardCast;
            try {
                type = readType();
                hardCast = optional(HardCastIndicatorOperator) != null;
                wexpect(CloseParen);
            }catch(TokenLocatedException e) {
                lexerTokens.setIndex(_start);
                var expr = readExpression();
                wexpect(CloseParen);
                return expr;
            }
            return new CastTree(start, curr(), type, hardCast, readPostExpression());
        }
        skipWhitespace();
        if(isNext(Identifier)) {
            return readAccessor();
        }
        if(isNext(String)) {
            return new LiteralTree(start, curr(), LiteralType.STRING, expect(String).data);
        }else if(optional(NullKeyword) != null) {
            return new LiteralTree(start, curr(), LiteralType.NULL, null);
        }else if(isNext(Integer)) {
            return new LiteralTree(start, curr(), LiteralType.INTEGER, ParserUtils.getInteger(expect(Integer)));
        }else if(optional(TrueKeyword) != null) {
            return new LiteralTree(start, curr(), LiteralType.BOOLEAN, true);
        }else if(optional(FalseKeyword) != null) {
            return new LiteralTree(start, curr(), LiteralType.BOOLEAN, false);
        }
        throw new TokenLocatedException("Expected an expression got " + lexerTokens.current().type, lexerTokens.current());
    }

    public AccessorTree readAccessor() {
        int start = curr();
        ArrayList<IAccessorElement> elements = new ArrayList<>();
        elements.add(readFunctionInvocationAccessor());
        while(skipWhitespace() && (isNext(OpenBracket) || isNext(AccessorOperator))) {
            if(isNext(OpenBracket)) {
                int startLoc = curr();
                expect(OpenBracket);
                IExpression expression = readExpression();
                expect(CloseBracket);
                elements.add(new ArrayAccessTree(startLoc, curr(), expression));
            }else {
                expect(AccessorOperator);
                elements.add(readFunctionInvocationAccessor());
            }
        }
        return new AccessorTree(start, curr(), elements.toArray(new IAccessorElement[0]));
    }

    public IAccessorElement readFunctionInvocationAccessor() {
        skipWhitespace();
        int start = curr();
        var ident = expect(Identifier);
        if(optional(OpenParen) != null) {
            ArgumentsTree args = readArgumentList(CloseParen);
            wexpect(CloseParen);
            return new FunctionCallTree(start, curr(), ident, args);
        }
        return new FieldAccessTree(start, curr(), ident);
    }

    public ArgumentsTree readArgumentList(TokenType endToken) {
        skipWhitespace();
        int start = curr();
        if(isNext(endToken)) {
            return new ArgumentsTree(start, curr(), new IExpression[0]);
        }
        ArrayList<IExpression> arguments = new ArrayList<>();
        arguments.add(readExpression());
        while(skipWhitespace() && !isNext(endToken)) {
            expect(CommaOperator);
            arguments.add(readExpression());
        }
        return new ArgumentsTree(start, curr(), arguments.toArray(new IExpression[0]));
    }

    // ---- Helper Methods ----

    public int curr() {
        return lexerTokens.currentIndex();
    }

    public boolean skipWhitespace() {
        while(lexerTokens.hasNext()) {
            LexerToken next = lexerTokens.seek();
            if(next.type != Whitespace && next.type != Comment) {
                return lexerTokens.hasNext();
            }
            lexerTokens.next();
        }
        return false;
    }

    public LexerToken expect(TokenType type) {
        if(!lexerTokens.hasNext()) {
            throw new TokenLocatedException("Expected '" + type.name() + "' got end of file", lexerTokens.current());
        }
        var next = lexerTokens.next();
        if(next.type == type) {
            return next;
        }
        throw new TokenLocatedException("Expected '" + type.name() + "', got '" + next.type + "'", next);
    }

    public LexerToken wexpect(TokenType type) {
        skipWhitespace();
        return expect(type);
    }

    public boolean isNext(TokenType type) {
        if(!lexerTokens.hasNext()) {
            return false;
        }
        return lexerTokens.seek().type == type;
    }

    public LexerToken optional(TokenType type) {
        if(!lexerTokens.hasNext()) {
            return null;
        }
        if(lexerTokens.seek().type == type) {
            return lexerTokens.next();
        }
        return null;
    }

    public LexerToken optional(TokenType... types) {
        for(TokenType type : types) {
            var opt = optional(type);
            if(opt == null) continue;
            return opt;
        }
        return null;
    }

}
