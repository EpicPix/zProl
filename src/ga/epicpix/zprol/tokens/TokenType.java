package ga.epicpix.zprol.tokens;

public enum TokenType {

    FIELD,
    FUNCTION, END_FUNCTION,
    OBJECT, END_OBJECT,
    OPEN_SCOPE, CLOSE_SCOPE,
    OPEN, CLOSE, COMMA, ACCESSOR, END_LINE,
    STRING, NUMBER, OPERATOR, WORD, LONG_WORD, DOT_WORD,
    KEYWORD, TYPE, EQUATION, PARSED

    // Remove: FIELD, FUNCTION, END_FUNCTION, OBJECT, END_OBJECT

}
