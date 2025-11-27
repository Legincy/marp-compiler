package pl.peth.common.scanner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pl.peth.common.Token;
import pl.peth.common.tokens.ITokenWrapper;

public class Scanner implements ITokenWrapper {
    private String input;
    private int position;
    private int line;
    private final List<Token> tokens;

    public Scanner() {
        this.input = "";
        this.position = 0;
        this.line = 1;
        this.tokens = new ArrayList<>();
    }

    public boolean scanFile(String fileName) {
        if(!readFile(fileName)) {
            return false;
        }
        return scan();
    }

    public boolean scan() {
        tokens.clear();
        position = 0;
        line = 1;

        System.out.println("Input length: " + input.length());


        try {
            int iterations = 0;
            while(position < input.length()) {
                iterations++;
                if (iterations > 10000) {
                    error(String.format("Too many iterations (%d). Possible infinite loop.", iterations));
                    return false;
                }

                char currentChar = charAtPosition();

                /*
                System.out.println("DEBUG: Iteration " + iterations +
                        ", pos=" + position +
                        ", char='" + currentChar + "'" +
                        " (ASCII: " + (int)currentChar + ")");
                */

                if(Character.isWhitespace(currentChar)){
                    if (currentChar == '\n') {
                        line++;
                    }
                    position++;
                    continue;
                }

                if (currentChar == '/' && charAtOneAfterPosition() == '/') {
                    skipComment();
                    continue;
                }

                if (Character.isDigit(currentChar)) {
                    tokens.add(scanNumber());
                } else if (Character.isLetter(currentChar) || currentChar == '_') {
                    tokens.add(scanIdentifierOrKeyword());
                }else {
                    Token token = scanOperatorOrDelimiter();
                    if (token != null) {
                        tokens.add(token);
                    } else {
                        error("Unrecognized character: " + currentChar);
                        return false;
                    }
                }
            }

            tokens.add(new Token(EOF_TOKEN, "EOF", line, position));
            return true;
        } catch (Exception ex) {
            error("Scanning failed: " + ex.getMessage());
            return false;
        }
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public void printTokens() {
        System.out.println("=== Scanner::Tokens ===");
        for (Token token : tokens) {
            System.out.println(token);
        }
    }

    private Token scanNumber() {
        int startLine = line;
        StringBuilder sb = new StringBuilder();

        while (position < input.length() && Character.isDigit(charAtPosition())) {
            sb.append(charAtPosition());
            position++;
        }

        return new Token(NUMERIC, sb.toString(), startLine, position);
    }

    private Token scanIdentifierOrKeyword() {
        int startLine = line;
        StringBuilder sb = new StringBuilder();

        while (position < input.length() && (Character.isLetterOrDigit(charAtPosition()) || charAtPosition() == '_')) {
            sb.append(charAtPosition());
            position++;
        }

        String lexeme = sb.toString();

        byte type = checkKeyword(lexeme);

        return new Token(type, lexeme, startLine, position);
    }

    private Token scanOperatorOrDelimiter() {
        int startLine = line;
        char currentChar = charAtPosition();

        switch(currentChar) {
            case '+':
                position++;
                return new Token(PLUS, "+", startLine, position);
            case '*':
                position++;
                return new Token(MULTIPLY, "*", startLine, position);
            case '/':
                position++;
                return new Token(DIVIDE, "/", startLine, position);
            case '-':
                position++;
                if(charAtPosition() == '>') {
                    position++;
                    return new Token(ARROW, "->", startLine, position);
                }
                return new Token(MINUS, "-", startLine, position);
            case '(':
                position++;
                return new Token(OPEN_PARENTHESIS, "(", startLine, position);
            case ')':
                position++;
                return new Token(CLOSE_PARENTHESIS, ")", startLine, position);
            case '{':
                position++;
                return new Token(OPEN_BRACE, "{", startLine, position);
            case '}':
                position++;
                return new Token(CLOSE_BRACE, "}", startLine, position);
            case ',':
                position++;
                return new Token(COMMA, ",", startLine, position);
            case ':':
                position++;
                return new Token(COLON, ":", startLine, position);
            case '=':
                position++;
                if (charAtPosition() == '=') {
                    position++;
                    return new Token(EQUAL, "==", startLine, position);
                }
                error("Unexpected '=' - did you mean '=='?");
                return null;
            case '!':
                position++;
                if (charAtPosition() == '=') {
                    position++;
                    return new Token(NOT_EQUAL, "!=", startLine, position);
                }
                error("Unexpected '!' - did you mean '!='?");
                return null;
            case '<':
                position++;
                if (charAtPosition() == '=') {
                    position++;
                    return new Token(LESS_EQUAL, "<=", startLine, position);
                }
                return new Token(LESS_THAN, "<", startLine, position);
            case '>':
                position++;
                if (charAtPosition() == '=') {
                    position++;
                    return new Token(GREATER_EQUAL, ">=", startLine, position);
                }
                return new Token(GREATER_THAN, ">", startLine, position);
            default:
                return null;
        }
    }

    private byte checkKeyword(String lexeme) {
        return switch(lexeme) {
            case "fn" -> FN;
            case "int" -> TYPE_INT;
            case "void" -> TYPE_VOID;
            case "bool" -> TYPE_BOOL;
            case "string" -> TYPE_STRING;
            case "return" -> RETURN;
            case "if" -> IF;
            case "else" -> ELSE;
            case "elseif" -> ELSE_IF;
            case "while" -> WHILE;
            default -> IDENTIFIER;
        };
    }

    private void skipComment() {
        while (position < input.length() && charAtPosition() != '\n') {
            position++;
        }
    }

    private boolean readFile(String fileName) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            StringBuilder sb = new StringBuilder();
            String line;

            while((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }

            reader.close();
            input = sb.toString();
            return true;
        } catch (IOException ex) {
            error("Error reading file: " + fileName);
            return false;
        }
    }

    public void setInput(String input) {
        this.input = input;
    }

    private char charAtPosition() {
        if (position >= input.length()) {
            return '\0';
        }
        return input.charAt(position);
    }

    private char charAtOneAfterPosition() {
        if (position + 1 >= input.length()) {
            return '\0';
        }
        return input.charAt(position + 1);
    }

    private void error(String message) {
        String errorMessage = String.format("Scanner Error (line %d): %s", line, message);
        System.err.println(errorMessage);
    }
}
