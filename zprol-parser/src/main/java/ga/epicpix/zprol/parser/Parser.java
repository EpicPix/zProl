package ga.epicpix.zprol.parser;

import ga.epicpix.zprol.errors.*;
import ga.epicpix.zprol.parser.exceptions.TokenLocatedException;
import ga.epicpix.zprol.parser.tokens.LexerToken;
import ga.epicpix.zprol.parser.tokens.TokenType;
import ga.epicpix.zprol.parser.tree.*;
import ga.epicpix.zprol.utils.SeekIterator;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;

import static ga.epicpix.zprol.parser.tokens.TokenType.*;

public final class Parser {

    private final SeekIterator<LexerToken> lexerTokens;
    private final ErrorStorage errors;
    private Parser(SeekIterator<LexerToken> lexerTokens, ErrorStorage errors) {
        this.lexerTokens = lexerTokens;
        this.errors = errors;
    }

    public static FileTree parse(SeekIterator<LexerToken> lexerTokens, ErrorStorage errors) {
        try {
            return new Parser(lexerTokens, errors).parse();
        }catch(CriticalErrorException e) {
            return null;
        }
    }

    public FileTree parse() {
        ArrayList<IDeclaration> declarations = new ArrayList<>();
        int _start = curr();
        while(skipWhitespace()) {
            int start = curr();
            switch(lexerTokens.seek().type) {
                case NamespaceKeyword: {
                    lexerTokens.next();
                    NamespaceIdentifierTree identifier = readNamespaceIdentifier(start);
                    wexpect(Semicolon);
                    declarations.add(new NamespaceTree(locS(start), locE(curr()), identifier));
                    break;
                }
                case UsingKeyword: {
                    lexerTokens.next();
                    NamespaceIdentifierTree identifier = readNamespaceIdentifier(start);
                    wexpect(Semicolon);
                    declarations.add(new UsingTree(locS(start), locE(curr()), identifier));
                    break;
                }
                case ClassKeyword:
                    declarations.add(readClass(start));
                    break;
                default:
                    declarations.add(readFieldOrMethod());
                    break;
            }
        }
        return new FileTree(locS(_start), locE(curr()), declarations);
    }

    public NamespaceIdentifierTree readNamespaceIdentifier(int start) {
        ArrayList<LexerToken> tokens = new ArrayList<>();
        tokens.add(wexpect(Identifier));
        while(optional(AccessorOperator) != null) {
            tokens.add(expect(Identifier));
        }
        return new NamespaceIdentifierTree(locS(start), locE(curr()), tokens.toArray(new LexerToken[0]));
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
        return new ClassTree(locS(start), locE(curr()), name, declarations);
    }

    public FunctionTree readFunction(int start, ModifiersTree mods, TypeTree type, LexerToken name) {
        wexpect(OpenParen);
        ParametersTree params = readParameterList(CloseParen);
        wexpect(CloseParen);
        skipWhitespace();
        if(optional(Semicolon) != null) {
            return new FunctionTree(locS(start), locE(curr()), mods, type, name, params, null);
        }
        CodeTree code = readCode();
        return new FunctionTree(locS(start), locE(curr()), mods, type, name, params, code);
    }

    public IDeclaration readFieldOrMethod() {
        int start = curr();
        if(skipWhitespace() && optional(ConstKeyword) != null) {
            TypeTree type = readType();
            LexerToken name = wexpect(Identifier);
            wexpect(AssignOperator);
            IExpression expression = readExpression();
            wexpect(Semicolon);
            return new FieldTree(locS(start), locE(curr()), true, type, name, expression);
        }
        ModifiersTree mods = readFunctionModifiers();
        TypeTree type = readType();
        LexerToken name = wexpect(Identifier);
        skipWhitespace();
        if(mods.modifiers.length != 0 || isNext(OpenParen)) {
            return readFunction(start, mods, type, name);
        }
        wexpect(Semicolon);
        return new FieldTree(locS(start), locE(curr()), false, type, name, null);
    }

    public ModifiersTree readFunctionModifiers() {
        ArrayList<ModifierTree> mods = new ArrayList<>();
        int start = curr();
        while(skipWhitespace()) {
            if(isNext(NativeKeyword)) {
                int localStart = curr();
                expect(NativeKeyword);
                mods.add(new ModifierTree(locS(localStart), locE(curr()), ModifierTree.NATIVE));
                continue;
            }
            if(!isNext(NativeKeyword)) {
                break;
            }
        }
        return new ModifiersTree(locS(start), locE(curr()), mods.toArray(new ModifierTree[0]));
    }

    public TypeTree readType() {
        skipWhitespace();
        int start = curr();
        int arrays = 0;
        if(isNext(VoidKeyword)) {
            return new TypeTree(locS(start), locE(curr()), expect(VoidKeyword), 0);
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
            return new TypeTree(locS(start), locE(curr()), token, arrays);
        }
    }

    public ParametersTree readParameterList(TokenType endToken) {
        skipWhitespace();
        int start = curr();
        if(isNext(endToken)) {
            return new ParametersTree(locS(start), locE(curr()), new ParameterTree[0]);
        }
        ArrayList<ParameterTree> params = new ArrayList<>();
        params.add(readParameter());
        while(skipWhitespace() && !isNext(endToken)) {
            expect(CommaOperator);
            params.add(readParameter());
        }
        return new ParametersTree(locS(start), locE(curr()), params.toArray(new ParameterTree[0]));
    }

    public ParameterTree readParameter() {
        skipWhitespace();
        int start = curr();
        TypeTree type = readType();
        errors.startCapturingErrors();
        LexerToken name = wtexpect(Identifier);
        if(errors.hasCapturedErrors(ErrorType.ERROR)) {
            errors.stopCapturingErrors(false);
            errors.addError(ErrorCodes.PARSE_PARAM_REQUIRED_NAME, name.toErrorLocation(lexerTokens.last().parser));
        }else {
            errors.stopCapturingErrors(false);
        }
        return new ParameterTree(locS(start), locE(curr()), type, name);
    }

    public CodeTree readCode() {
        skipWhitespace();
        int start = curr();
        if(!isNext(OpenBrace)) {
            errors.startCapturingErrors();
            LexerToken loc = texpect(LineCodeChars);
            if(errors.hasCapturedErrors(ErrorType.ERROR)) {
                errors.stopCapturingErrors(false);
                DataParser parser = lexerTokens.current().parser;
                ParserLocation s = parser.getLocation(loc.getStart());
                errors.addError(ErrorCodes.PARSE_CODE_OPEN_BRACE_OR_LINE_CODE, new ErrorLocation(s.row, s.line, parser.getFileName(), parser.getLines()), "=> ");
            }else {
                errors.stopCapturingErrors(false);
            }
            IStatement statement = readStatement();
            return new CodeTree(locS(start), locE(curr()), new ArrayList<>(Collections.singleton(statement)));
        }
        expect(OpenBrace);
        ArrayList<IStatement> statements = new ArrayList<>();
        while(skipWhitespace() && !isNext(CloseBrace)) {
            statements.add(readStatement());
        }
        skipWhitespace();
        if(lexerTokens.get(lexerTokens.currentIndex() - 1).type != CloseBrace) {
            expect(CloseBrace);
        }
        return new CodeTree(locS(start), locE(curr()), statements);
    }

    public BreakStatementTree readBreakStatement() {
        skipWhitespace();
        int start = curr();
        expect(BreakKeyword);
        wexpect(Semicolon);
        return new BreakStatementTree(locS(start), locE(curr()));
    }

    public ContinueStatementTree readContinueStatement() {
        skipWhitespace();
        int start = curr();
        expect(ContinueKeyword);
        wexpect(Semicolon);
        return new ContinueStatementTree(locS(start), locE(curr()));
    }

    public ReturnStatementTree readReturnStatement() {
        skipWhitespace();
        int start = curr();
        expect(ReturnKeyword);
        skipWhitespace();
        if(optional(Semicolon) != null) {
            return new ReturnStatementTree(locS(start), locE(curr()), null);
        }
        IExpression expression = readExpression();
        wexpect(Semicolon);
        return new ReturnStatementTree(locS(start), locE(curr()), expression);
    }

    public WhileStatementTree readWhileStatement() {
        skipWhitespace();
        int start = curr();
        wexpect(WhileKeyword);
        wexpect(OpenParen);
        IExpression expression = readExpression();
        wexpect(CloseParen);
        CodeTree code = readCode();
        return new WhileStatementTree(locS(start), locE(curr()), expression, code);
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
        return new IfStatementTree(locS(start), locE(curr()), expression, code, elseStatement);
    }

    public ElseStatementTree readElseStatement() {
        skipWhitespace();
        int start = curr();
        expect(ElseKeyword);
        CodeTree code = readCode();
        return new ElseStatementTree(locS(start), locE(curr()), code);
    }

    public CreateAssignmentStatementTree readCreateAssignmentStatement(int start, TypeTree type) {
        LexerToken name = wexpect(Identifier);
        wexpect(AssignOperator);
        IExpression expression = readExpression();
        wexpect(Semicolon);
        return new CreateAssignmentStatementTree(locS(start), locE(curr()), type, name, expression);
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
            errors.startCapturingErrors();
            TypeTree type = readType();
            if(errors.hasCapturedErrors(ErrorType.ERROR)) {
                errors.stopCapturingErrors(false);
                lexerTokens.setIndex(start);
                readAccessor();
            }else {
                errors.stopCapturingErrors(false);
            }
            skipWhitespace();
            if(!isNext(Identifier)) {
                lexerTokens.setIndex(start);
                AccessorTree accessor = readAccessor();
                skipWhitespace();
                if(optional(AssignOperator) != null) {
                    IExpression expression = readExpression();
                    wexpect(Semicolon);
                    return new AssignmentStatementTree(locS(start), locE(curr()), accessor, expression);
                }else {
                    wexpect(Semicolon);
                    return new AccessorStatementTree(locS(start), locE(curr()), accessor);
                }
            }else {
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
        ArrayList<OperatorExpression> operators = null;
        LexerToken operator;
        while(skipWhitespace() && (operator = optional(AndOperator)) != null) {
            if(operators == null) operators = new ArrayList<>();
            operators.add(new OperatorExpression(operator, readInclusiveOrExpression()));
        }
        if(operators == null) {
            return expression;
        }else {
            return new OperatorExpressionTree(locS(start), locE(curr()), expression, operators.toArray(new OperatorExpression[0]));
        }
    }

    public IExpression readInclusiveOrExpression() {
        skipWhitespace();
        int start = curr();
        IExpression expression = readShiftExpression();
        ArrayList<OperatorExpression> operators = null;
        LexerToken operator;
        while(skipWhitespace() && (operator = optional(InclusiveOrOperator)) != null) {
            if(operators == null) operators = new ArrayList<>();
            operators.add(new OperatorExpression(operator, readShiftExpression()));
        }
        if(operators == null) {
            return expression;
        }else {
            return new OperatorExpressionTree(locS(start), locE(curr()), expression, operators.toArray(new OperatorExpression[0]));
        }
    }

    public IExpression readShiftExpression() {
        skipWhitespace();
        int start = curr();
        IExpression expression = readEqualsExpression();
        ArrayList<OperatorExpression> operators = null;
        LexerToken operator;
        while(skipWhitespace() && (operator = optional(ShiftLeftOperator, ShiftRightOperator)) != null) {
            if(operators == null) operators = new ArrayList<>();
            operators.add(new OperatorExpression(operator, readEqualsExpression()));
        }
        if(operators == null) {
            return expression;
        }else {
            return new OperatorExpressionTree(locS(start), locE(curr()), expression, operators.toArray(new OperatorExpression[0]));
        }
    }


    public IExpression readEqualsExpression() {
        skipWhitespace();
        int start = curr();
        IExpression expression = readCompareExpression();
        ArrayList<OperatorExpression> operators = null;
        LexerToken operator;
        while(skipWhitespace() && (operator = optional(EqualOperator, NotEqualOperator)) != null) {
            if(operators == null) operators = new ArrayList<>();
            operators.add(new OperatorExpression(operator, readCompareExpression()));
        }
        if(operators == null) {
            return expression;
        }else {
            return new OperatorExpressionTree(locS(start), locE(curr()), expression, operators.toArray(new OperatorExpression[0]));
        }
    }


    public IExpression readCompareExpression() {
        skipWhitespace();
        int start = curr();
        IExpression expression = readAdditiveExpression();
        ArrayList<OperatorExpression> operators = null;
        LexerToken operator;
        while(skipWhitespace() && (operator = optional(LessEqualThanOperator, LessThanOperator, GreaterEqualThanOperator, GreaterThanOperator)) != null) {
            if(operators == null) operators = new ArrayList<>();
            operators.add(new OperatorExpression(operator, readAdditiveExpression()));
        }
        if(operators == null) {
            return expression;
        }else {
            return new OperatorExpressionTree(locS(start), locE(curr()), expression, operators.toArray(new OperatorExpression[0]));
        }
    }


    public IExpression readAdditiveExpression() {
        skipWhitespace();
        int start = curr();
        IExpression expression = readMultiplicativeExpression();
        ArrayList<OperatorExpression> operators = null;
        LexerToken operator;
        while(skipWhitespace() && (operator = optional(AddOperator, SubtractOperator)) != null) {
            if(operators == null) operators = new ArrayList<>();
            operators.add(new OperatorExpression(operator, readMultiplicativeExpression()));
        }
        if(operators == null) {
            return expression;
        }else {
            return new OperatorExpressionTree(locS(start), locE(curr()), expression, operators.toArray(new OperatorExpression[0]));
        }
    }


    public IExpression readMultiplicativeExpression() {
        skipWhitespace();
        int start = curr();
        IExpression expression = readPostExpression();
        ArrayList<OperatorExpression> operators = null;
        LexerToken operator;
        while(skipWhitespace() && (operator = optional(MultiplyOperator, DivideOperator, ModuloOperator)) != null) {
            if(operators == null) operators = new ArrayList<>();
            operators.add(new OperatorExpression(operator, readPostExpression()));
        }
        if(operators == null) {
            return expression;
        }else {
            return new OperatorExpressionTree(locS(start), locE(curr()), expression, operators.toArray(new OperatorExpression[0]));
        }
    }


    public IExpression readPostExpression() {
        skipWhitespace();
        int start = curr();
        if(optional(OpenParen) != null) {
            int _start = curr();
            errors.startCapturingErrors();
            TypeTree type = readType();
            boolean hardCast = optional(HardCastIndicatorOperator) != null;
            wexpect(CloseParen);
            if(errors.hasCapturedErrors(ErrorType.ERROR)) {
                errors.stopCapturingErrors(false);
                lexerTokens.setIndex(_start);
                IExpression expr = readExpression();
                wexpect(CloseParen);
                return expr;
            }else {
                errors.stopCapturingErrors(false);
            }
            return new CastTree(locS(start), locE(curr()), type, hardCast, readPostExpression());
        }
        skipWhitespace();
        if(isNext(Identifier)) {
            return readAccessor();
        }
        if(isNext(String)) {
            return new LiteralTree(locS(start), locE(curr()), LiteralType.STRING, expect(String).data);
        }else if(optional(NullKeyword) != null) {
            return new LiteralTree(locS(start), locE(curr()), LiteralType.NULL, null);
        }else if(isNext(Integer)) {
            BigInteger i = ParserUtils.getInteger(expect(Integer));
            return new LiteralTree(locS(start), locE(curr()), LiteralType.INTEGER, i);
        }else if(optional(TrueKeyword) != null) {
            return new LiteralTree(locS(start), locE(curr()), LiteralType.BOOLEAN, true);
        }else if(optional(FalseKeyword) != null) {
            return new LiteralTree(locS(start), locE(curr()), LiteralType.BOOLEAN, false);
        }else if(optional(SubtractOperator) != null) {
            IExpression expr = readPostExpression();
            return new NegateTree(locS(start), locE(curr()), expr);
        }
        ParserLocation loc = lexerTokens.current().parser.getLocation(lexerTokens.current().getStart());
        errors.addError(ErrorCodes.PARSE_EXPECTED_EXPRESSION_GOT_OTHER, new ErrorLocation(loc.row, loc.line, lexerTokens.current().parser.getFileName(), lexerTokens.current().parser.getLines()), lexerTokens.current().type);
        return new InvalidExpressionTree(locS(start), locE(curr()));
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
                elements.add(new ArrayAccessTree(locS(startLoc), locE(curr()), expression));
            }else {
                expect(AccessorOperator);
                elements.add(readFunctionInvocationAccessor());
            }
        }
        return new AccessorTree(locS(start), locE(curr()), elements.toArray(new IAccessorElement[0]));
    }

    public IAccessorElement readFunctionInvocationAccessor() {
        skipWhitespace();
        int start = curr();
        LexerToken ident = expect(Identifier);
        if(optional(OpenParen) != null) {
            ArgumentsTree args = readArgumentList(CloseParen);
            wtexpect(CloseParen);
            return new FunctionCallTree(locS(start), locE(curr()), ident, args);
        }
        return new FieldAccessTree(locS(start), locE(curr()), ident);
    }

    public ArgumentsTree readArgumentList(TokenType endToken) {
        skipWhitespace();
        int start = curr();
        if(isNext(endToken)) {
            return new ArgumentsTree(locS(start), locE(curr()), new IExpression[0]);
        }
        ArrayList<IExpression> arguments = new ArrayList<>();
        IExpression expr = readExpression();
        arguments.add(expr);
        if(expr instanceof InvalidExpressionTree) {
            DataParser parser = lexerTokens.current().parser;
            ParserLocation s = parser.getLocation(expr.getStartIndex());
            ParserLocation e = parser.getLocation(expr.getEndIndex());
            errors.addError(ErrorCodes.PARSE_ARGS_NOT_VALID_PAREN_OR_SEMICOLON, new ErrorLocation(s.row, s.line, e.row, e.line, parser.getFileName(), parser.getLines()), ")");
        }else {
            int last = curr();
            while(skipWhitespace() && !isNext(endToken)) {
                if(isNext(CloseBrace)) {
                    expect(endToken);
                    break;
                }
                if(expect(CommaOperator).type != Invalid) {
                    arguments.add(readExpression());
                    last = curr();
                }else {
                    break;
                }
            }
            lexerTokens.setIndex(last);
        }
        return new ArgumentsTree(locS(start), locE(curr()), arguments.toArray(new IExpression[0]));
    }

    // ---- Helper Methods ----

    public int curr() {
        return lexerTokens.currentIndex();
    }

    public int locS(int c) {
        return lexerTokens.get(c).getStart();
    }

    public int locE(int c) {
        if(c==0) c++;
        return lexerTokens.get(c-1).getEnd();
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

    public LexerToken texpect(TokenType type) {
        int cur = curr();
        LexerToken r = expect(type);
        if(r.type == Invalid) {
            lexerTokens.setIndex(cur);
        }
        return r;
    }

    public LexerToken expect(TokenType type) {
        LexerToken appendLoc = lexerTokens.hasPrevious() ? lexerTokens.get(lexerTokens.currentIndex() - 1) : lexerTokens.current();
        if(!lexerTokens.hasNext()) {
            if(type.token == null) {
                DataParser parser = lexerTokens.last().parser;
                ParserLocation lastLoc = parser.getLocation(lexerTokens.last().getEnd());
                errors.addError(ErrorCodes.PARSE_EXPECTED_VALUE_GOT_EOF, new ErrorLocation(lastLoc.row, lastLoc.line, parser.getFileName(), parser.getLines()), type.name());
                throw new TokenLocatedException("EOL");
            }else {
                ParserLocation loc = appendLoc.parser.getLocation(appendLoc.getEnd());
                errors.addError(ErrorCodes.PARSE_EXPECTED_VALUE_GOT_EOF_FIXABLE, new ErrorLocation(loc.row, loc.line, appendLoc.parser.getFileName(), appendLoc.parser.getLines()), type.name(), type.token);
                return new LexerToken(Invalid, type.token, appendLoc.getEnd(), appendLoc.getEnd() + type.token.length(), appendLoc.parser);
            }
        }
        LexerToken next = lexerTokens.next();
        if(next.type == type) {
            return next;
        }
        if(type.token == null) {
            ParserLocation sloc = appendLoc.parser.getLocation(next.getStart());
            ParserLocation eloc = appendLoc.parser.getLocation(next.getEnd());
            errors.addError(ErrorCodes.PARSE_EXPECTED_VALUE_GOT_OTHER, new ErrorLocation(sloc.row, sloc.line, eloc.row, eloc.line, appendLoc.parser.getFileName(), appendLoc.parser.getLines()), type.name(), next.type);
            return new LexerToken(Invalid, "", next.getStart(), next.getEnd(), lexerTokens.last().parser);
        }
        ParserLocation loc = appendLoc.parser.getLocation(appendLoc.getEnd());
        errors.addError(ErrorCodes.PARSE_EXPECTED_VALUE_GOT_OTHER_FIXABLE, new ErrorLocation(loc.row, loc.line, appendLoc.parser.getFileName(), appendLoc.parser.getLines()), type.name(), next.type, type.token);
        return new LexerToken(Invalid, type.token, appendLoc.getEnd(), appendLoc.getEnd() + type.token.length(), appendLoc.parser);
    }

    public LexerToken wexpect(TokenType type) {
        skipWhitespace();
        return expect(type);
    }

    public LexerToken wtexpect(TokenType type) {
        skipWhitespace();
        return texpect(type);
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
            LexerToken opt = optional(type);
            if(opt == null) continue;
            return opt;
        }
        return null;
    }

}
