package ga.epicpix.zprol.parser.tokens;

public enum TokenType {

    // Lexer Tokens

    Whitespace,
    Comment,

    LineCodeChars,

    NamespaceKeyword,
    UsingKeyword,
    ClassKeyword,
    ConstKeyword,
    NativeKeyword,
    VoidKeyword,
    BoolKeyword,
    BreakKeyword,
    ContinueKeyword,
    ReturnKeyword,
    WhileKeyword,
    IfKeyword,
    ElseKeyword,
    NullKeyword,

    Identifier,
    String,
    Integer,
    TrueKeyword,
    FalseKeyword,
    Semicolon,

    AccessorOperator,
    CommaOperator,
    AssignOperator,
    AndOperator,
    InclusiveOrOperator,
    ShiftLeftOperator,
    ShiftRightOperator,
    EqualOperator,
    NotEqualOperator,
    LessEqualThanOperator,
    LessThanOperator,
    GreaterEqualThanOperator,
    GreaterThanOperator,
    AddOperator,
    SubtractOperator,
    MultiplyOperator,
    DivideOperator,
    ModuloOperator,
    HardCastIndicatorOperator,

    OpenBrace,
    CloseBrace,

    OpenParen,
    CloseParen,

    OpenBracket,
    CloseBracket



}
