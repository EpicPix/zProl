package ga.epicpix.zprol.parser.tokens;

public enum TokenType {

    Whitespace(null),
    Comment(null),

    LineCodeChars("=>"),

    NamespaceKeyword("namespace"),
    UsingKeyword("using"),
    ClassKeyword("class"),
    ConstKeyword("const"),
    NativeKeyword("native"),
    VoidKeyword("void"),
    BoolKeyword("bool"),
    BreakKeyword("break"),
    ContinueKeyword("continue"),
    ReturnKeyword("return"),
    WhileKeyword("while"),
    IfKeyword("if"),
    ElseKeyword("else"),
    NullKeyword("null"),

    Identifier(null),
    String(null),
    Integer(null),
    TrueKeyword("true"),
    FalseKeyword("false"),
    Semicolon(";"),

    AccessorOperator("."),
    CommaOperator(","),
    AssignOperator("="),
    AndOperator("&"),
    InclusiveOrOperator("|"),
    ShiftLeftOperator("<<"),
    ShiftRightOperator(">>"),
    EqualOperator("=="),
    NotEqualOperator("!="),
    LessEqualThanOperator("<="),
    LessThanOperator("<"),
    GreaterEqualThanOperator(">="),
    GreaterThanOperator(">"),
    AddOperator("+"),
    SubtractOperator("-"),
    MultiplyOperator("*"),
    DivideOperator("/"),
    ModuloOperator("%"),
    HardCastIndicatorOperator("!"),

    OpenBrace("{"),
    CloseBrace("}"),

    OpenParen("("),
    CloseParen(")"),

    OpenBracket("["),
    CloseBracket("]"),


    Invalid(null);

    public final String token;

    TokenType(String token) {
        this.token = token;
    }


}
