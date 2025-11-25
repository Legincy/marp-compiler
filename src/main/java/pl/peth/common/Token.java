package pl.peth.common;

import pl.peth.common.tokens.ITokenWrapper;


public class Token implements ITokenWrapper {
    byte type;
    String lexeme;
    int line;
    int position;

    public Token(byte type, String lexeme, int line, int position) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.position = position;
    }

    public Token(byte type, String lexeme) {
        this(type, lexeme, 1, 0);
    }

    @Override
    public String toString() {
        String typeAsByte = String.format("%8s", Integer.toBinaryString(type & 0xFF)).replace(' ', '0');
        return String.format("Token(type=BYTE(%s) [%s], lexeme='%s', line=%d, position=%d)", type, typeAsByte, lexeme, line, position);
    }

    public String getTokenName() {
        return getTokenName(type);
    }

    public static String getTokenName(byte type) {
        return switch (type) {
            //IBasicTokens
            case NO_TYPE -> "NO_TYPE";
            case EOF_TOKEN -> "EOF_TOKEN";
            case EPSILON -> "EPSILON";
            case START -> "START";
            case NOT_FINAL -> "NOT_FINAL";
            //IExpressionTokens
            case EXPRESSION -> "EXPRESSION";
            case RIGHT_EXPRESSION -> "RIGHT_EXPRESSION";
            case TERM -> "TERM";
            case RIGHT_TERM -> "RIGHT_TERM";
            case FACTOR -> "FACTOR";
            case FUNCTION_CALL -> "FUNCTION_CALL";
            case EXPRESSION_LIST -> "EXPRESSION_LIST";
            //IIdentifierTokens
            case IDENTIFIER -> "IDENTIFIER";
            case TYPE_INT -> "TYPE_INT";
            //IKeywordTokens
            case FN -> "FN";
            case RETURN -> "RETURN";
            case IF -> "IF";
            case ELSE -> "ELSE";
            case ELSE_IF -> "ELSE_IF";
            //ILiteralTokens
            case NUMERIC -> "NUMERIC";
            //INonTerminalTokens
            case PROGRAM -> "PROGRAM";
            case FUNCTION -> "FUNCTION";
            case PARAMETER_LIST -> "PARAMETER_LIST";
            case PARAMETER -> "PARAMETER";
            case BLOCK -> "BLOCK";
            case RETURN_STATEMENT -> "RETURN_STATEMENT";
            case IF_STATEMENT -> "IF_STATEMENT";
            case ELSE_IF_STATEMENT -> "ELSE_IF_STATEMENT";
            case ELSE_STATEMENT -> "ELSE_STATEMENT";
            case CONDITION -> "CONDITION";
            //IOperatorTokens
            case PLUS -> "PLUS";
            case MINUS -> "MINUS";
            case MULTIPLY -> "MULTIPLY";
            case DIVIDE -> "DIVIDE";
            case EQUAL -> "EQUAL";
            case NOT_EQUAL -> "NOT_EQUAL";
            case GREATER_THAN -> "GREATER_THAN";
            case LESS_THAN -> "LESS_THAN";
            case GREATER_EQUAL -> "GREATER_EQUAL";
            case LESS_EQUAL -> "LESS_EQUAL";
            //ISeparatorTokens
            case COMMA -> "COMMA";
            case COLON -> "COLON";
            case OPEN_PARENTHESIS -> "OPEN_PARENTHESIS";
            case CLOSE_PARENTHESIS -> "CLOSE_PARENTHESIS";
            case OPEN_BRACE -> "OPEN_BRACE";
            case CLOSE_BRACE -> "CLOSE_BRACE";
            case ARROW -> "ARROW";
            default -> "UNKNOWN_TOKEN";
        };
    }

    String getTokenName(int type) {
        return switch (type) {
            //IConstantTokens
            case  UNDEFINED -> "UNDEFINED";
            default -> "UNKNOWN_TOKEN";
        };
    }

    public byte getType() {
        return type;
    }

    public String getLexeme() {
        return lexeme;
    }

    public int getLine() {
        return line;
    }

    public int getPosition() {
        return position;
    }
}