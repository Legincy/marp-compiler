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

    String getTokenName(byte type) {
        return switch (type) {
            //IBasicTokens
            case ITokenWrapper.NO_TYPE -> "NO_TYPE";
            case ITokenWrapper.EOF_TOKEN -> "EOF_TOKEN";
            case ITokenWrapper.EPSILON -> "EPSILON";
            case ITokenWrapper.START -> "START";
            case ITokenWrapper.NOT_FINAL -> "NOT_FINAL";
            //IExpressionTokens
            case ITokenWrapper.EXPRESSION -> "EXPRESSION";
            case ITokenWrapper.RIGHT_EXPRESSION -> "RIGHT_EXPRESSION";
            case ITokenWrapper.TERM -> "TERM";
            case ITokenWrapper.RIGHT_TERM -> "RIGHT_TERM";
            case ITokenWrapper.FACTOR -> "FACTOR";
            case ITokenWrapper.FUNCTION_CALL -> "FUNCTION_CALL";
            case ITokenWrapper.EXPRESSION_LIST -> "EXPRESSION_LIST";
            //IIdentifierTokens
            case ITokenWrapper.IDENTIFIER -> "IDENTIFIER";
            case ITokenWrapper.TYPE_INT -> "TYPE_INT";
            //IKeywordTokens
            case ITokenWrapper.RETURN -> "RETURN";
            //ILiteralTokens
            case ITokenWrapper.NUMERIC -> "NUMERIC";
            //INonTerminalTokens
            case ITokenWrapper.PROGRAM -> "PROGRAM";
            case ITokenWrapper.FUNCTION -> "FUNCTION";
            case ITokenWrapper.PARAMETER_LIST -> "PARAMETER_LIST";
            case ITokenWrapper.PARAMETER -> "PARAMETER";
            case ITokenWrapper.BLOCK -> "BLOCK";
            case ITokenWrapper.RETURN_STATEMENT -> "RETURN_STATEMENT";
            //IOperatorTokens
            case ITokenWrapper.PLUS -> "PLUS";
            case ITokenWrapper.MINUS -> "MINUS";
            case ITokenWrapper.MULTIPLY -> "MULTIPLY";
            case ITokenWrapper.DIVIDE -> "DIVIDE";
            //ISeparatorTokens
            case ITokenWrapper.COMMA -> "COMMA";
            case ITokenWrapper.COLON -> "COLON";
            case ITokenWrapper.OPEN_PARENTHESIS -> "OPEN_PARENTHESIS";
            case ITokenWrapper.CLOSE_PARENTHESIS -> "CLOSE_PARENTHESIS";
            case ITokenWrapper.OPEN_BRACE -> "OPEN_BRACE";
            case ITokenWrapper.CLOSE_BRACE -> "CLOSE_BRACE";
            case ITokenWrapper.ARROW -> "ARROW";
            default -> "UNKNOWN_TOKEN";
        };
    }

    String getTokenName(int type) {
        return switch (type) {
            //IConstantTokens
            case  ITokenWrapper.UNDEFINED -> "UNDEFINED";
            default -> "UNKNOWN_TOKEN";
        };
    }

    public byte getType() {
        return type;
    }
}