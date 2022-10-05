package ga.epicpix.zprol.parser;

import ga.epicpix.zprol.parser.exceptions.ParserException;
import ga.epicpix.zprol.parser.tokens.LexerToken;
import java.util.ArrayList;

import static ga.epicpix.zprol.parser.tokens.TokenType.*;

public class Lexer {

    public static ArrayList<LexerToken> lex(String filename, String[] lines) {
        DataParser parser = new DataParser(filename, lines);
        ArrayList<LexerToken> tokens = new ArrayList<>();
        while(parser.hasNext()) {
            var lex = lexNext(parser);
            if(lex != null) {
                tokens.add(lex);
            }
        }
        return tokens;
    }

    public static LexerToken lexNext(DataParser parser) {
        var start = parser.getIndex();
        var first = parser.nextChar();
        if((first >= 'a' && first <= 'z') || (first >= 'A' && first <= 'Z') || first == '_') {
            int value;
            while((value = parser.nextChar()) != -1) {
                if(!((value >= 'a' && value <= 'z') || (value >= 'A' && value <= 'Z') || (value >= '0' && value <= '9') || value == '_')) {
                    parser.goBack();
                    break;
                }
            }
            var end = parser.getIndex();
            var str = parser.data.substring(start, end);
            return switch(str) {
                case "namespace" -> new LexerToken(NamespaceKeyword, str, start, end, parser);
                case "using" -> new LexerToken(UsingKeyword, str, start, end, parser);
                case "class" -> new LexerToken(ClassKeyword, str, start, end, parser);
                case "return" -> new LexerToken(ReturnKeyword, str, start, end, parser);
                case "if" -> new LexerToken(IfKeyword, str, start, end, parser);
                case "else" -> new LexerToken(ElseKeyword, str, start, end, parser);
                case "while" -> new LexerToken(WhileKeyword, str, start, end, parser);
                case "break" -> new LexerToken(BreakKeyword, str, start, end, parser);
                case "continue" -> new LexerToken(ContinueKeyword, str, start, end, parser);
                case "true" -> new LexerToken(TrueKeyword, str, start, end, parser);
                case "false" -> new LexerToken(FalseKeyword, str, start, end, parser);
                case "null" -> new LexerToken(NullKeyword, str, start, end, parser);
                case "void" -> new LexerToken(VoidKeyword, str, start, end, parser);
                case "bool" -> new LexerToken(BoolKeyword, str, start, end, parser);
                case "native" -> new LexerToken(NativeKeyword, str, start, end, parser);
                case "const" -> new LexerToken(ConstKeyword, str, start, end, parser);
                default -> new LexerToken(Identifier, str, start, end, parser);
            };
        }else if(first == ' ' || first == '\t' || first == '\n') {
            int value;
            while((value = parser.nextChar()) != -1) {
                if(!(value == ' ' || value == '\t' || value == '\n')) {
                    parser.goBack();
                    break;
                }
            }
            return new LexerToken(Whitespace, "", start, parser.getIndex(), parser);
        }else if(first >= '0' && first <= '9') {
            int value = parser.nextChar();
            if(value == 'x' || value == 'X') {
                while((value = parser.nextChar()) != -1) {
                    if(!((value >= '0' && value <= '9') || (value >= 'a' && value <= 'f') || (value >= 'A' && value <= 'F'))) {
                        parser.goBack();
                        break;
                    }
                }
                if(parser.getIndex() - start <= 2) throw new ParserException("Expected a hex number after '0x'", parser);
            }else {
                parser.goBack();
                while((value = parser.nextChar()) != -1) {
                    if(!(value >= '0' && value <= '9')) {
                        parser.goBack();
                        break;
                    }
                }
            }
            return new LexerToken(Integer, parser.data.substring(start, parser.getIndex()), start, parser.getIndex(), parser);
        }else if(first == '"') {
            int value;
            StringBuilder str = new StringBuilder();
            while((value = parser.nextChar()) != -1) {
                if(value == '\n') {
                    throw new ParserException("Unexpected new line after string", parser);
                }
                if(value == '"') {
                    break;
                }
                if(value == '\\') {
                    int check = parser.nextChar();
                    switch(check) {
                        case '\\' -> str.append("\\");
                        case 'n' -> str.append("\n");
                        case 'r' -> str.append("\r");
                        case 'f' -> str.append("\f");
                        case '\'' -> str.append("'");
                        case 't' -> str.append("\t");
                        case 'b' -> str.append("\b");
                        case '0' -> str.append("\0");
                        case '"' -> str.append("\"");
                        default -> {
                            parser.goBack();
                            throw new ParserException("Unexpected escape sequence", parser);
                        }
                    }
                    continue;
                }
                str.appendCodePoint(value);
            }
            return new LexerToken(String, str.toString(), start, parser.getIndex(), parser);
        }else {
            switch(first) {
                case ';' -> {return new LexerToken(Semicolon, ";", start, parser.getIndex(), parser);}
                case '[' -> {return new LexerToken(OpenBracket, "[", start, parser.getIndex(), parser);}
                case ']' -> {return new LexerToken(CloseBracket, "]", start, parser.getIndex(), parser);}
                case '{' -> {return new LexerToken(OpenBrace, "{", start, parser.getIndex(), parser);}
                case '}' -> {return new LexerToken(CloseBrace, "}", start, parser.getIndex(), parser);}
                case '(' -> {return new LexerToken(OpenParen, "(", start, parser.getIndex(), parser);}
                case ')' -> {return new LexerToken(CloseParen, ")", start, parser.getIndex(), parser);}
                case ',' -> {return new LexerToken(CommaOperator, ",", start, parser.getIndex(), parser);}
                case '.' -> {return new LexerToken(AccessorOperator, ".", start, parser.getIndex(), parser);}
                case '+' -> {return new LexerToken(AddOperator, "+", start, parser.getIndex(), parser);}
                case '-' -> {return new LexerToken(SubtractOperator, "-", start, parser.getIndex(), parser);}
                case '*' -> {return new LexerToken(MultiplyOperator, "*", start, parser.getIndex(), parser);}
                case '/' -> {
                    int next = parser.nextChar();
                    if(next == '/') {
                        int value;
                        while((value = parser.nextChar()) != -1) {
                            if(value == '\n') {
                                parser.goBack();
                                break;
                            }
                        }
                        return new LexerToken(Comment, "", start, parser.getIndex(), parser);
                    }
                    parser.goBack();
                    return new LexerToken(DivideOperator, "/", start, parser.getIndex(), parser);
                }
                case '%' -> {return new LexerToken(ModuloOperator, "%", start, parser.getIndex(), parser);}
                case '|' -> {return new LexerToken(InclusiveOrOperator, "|", start, parser.getIndex(), parser);}
                case '&' -> {return new LexerToken(AndOperator, "&", start, parser.getIndex(), parser);}
                case '!' -> {
                    int next = parser.nextChar();
                    if(next == '=') {
                        return new LexerToken(NotEqualOperator, "!=", start, parser.getIndex(), parser);
                    }
                    parser.goBack();
                    return new LexerToken(HardCastIndicatorOperator, "!", start, parser.getIndex(), parser);
                }
                case '=' -> {
                    int next = parser.nextChar();
                    if(next == '=') {
                        return new LexerToken(EqualOperator, "==", start, parser.getIndex(), parser);
                    }else if(next == '>') {
                        return new LexerToken(LineCodeChars, "=>", start, parser.getIndex(), parser);
                    }
                    parser.goBack();
                    return new LexerToken(AssignOperator, "=", start, parser.getIndex(), parser);
                }
                case '>' -> {
                    int next = parser.nextChar();
                    if(next == '=') {
                        return new LexerToken(GreaterEqualThanOperator, ">=", start, parser.getIndex(), parser);
                    }else if(next == '>') {
                        return new LexerToken(ShiftRightOperator, ">>", start, parser.getIndex(), parser);
                    }
                    parser.goBack();
                    return new LexerToken(GreaterThanOperator, ">", start, parser.getIndex(), parser);
                }
                case '<' -> {
                    int next = parser.nextChar();
                    if(next == '=') {
                        return new LexerToken(LessEqualThanOperator, "<=", start, parser.getIndex(), parser);
                    }else if(next == '<') {
                        return new LexerToken(ShiftLeftOperator, "<<", start, parser.getIndex(), parser);
                    }
                    parser.goBack();
                    return new LexerToken(LessThanOperator, "<", start, parser.getIndex(), parser);
                }
            }
        }
        parser.goBack();
        throw new ParserException("Unknown token", parser, parser.getLocation());
    }

}
