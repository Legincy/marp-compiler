package pl.peth.common.parser;

import java.util.List;

import pl.peth.common.Token;
import pl.peth.common.scanner.Scanner;
import pl.peth.common.tokens.ITokenWrapper;

public class Parser implements ITokenWrapper {
    private List<Token> tokens;
    private int position;
    private Token currentToken;

    public Parser() {
        this.tokens = null;
        this.position = 0;
        this.currentToken = null;
    }

    public SyntaxTree parse(Scanner scanner) {
        this.tokens = scanner.getTokens();
        this.position = 0;

        if(this.tokens == null || this.tokens.isEmpty()) {
            error("No tokens to parse.");
            return null;
        }

        this.currentToken = this.tokens.get(0);

        SyntaxTree syntaxTree = new SyntaxTree(PROGRAM);
        
        if(!program(syntaxTree)) {
            return null;
        }

        if(currentToken.getType() != EOF_TOKEN) {
            error("Unexpected token after program end: " + currentToken);
            return null;
        }

        return syntaxTree;
    }

    private boolean program(SyntaxTree node) {
        return statements(node);
    }

    private boolean statements(SyntaxTree node) {
        if(check(EOF_TOKEN) || check(CLOSE_BRACE)) {
            return true;
        }

        if(!statement(node)){
            return false;
        }

        return statements(node);
    }

    private boolean statement(SyntaxTree node) {
        if (check(FN)) {
            return function(node.insertChild(FUNCTION));
        }

        if (check(IF)){
            return ifStatement(node.insertChild(IF_STATEMENT));
        }

        if (check(RETURN)){
            return returnStatement(node.insertChild(RETURN_STATEMENT));
        }

        return expression(node.insertChild(EXPRESSION));
    }

    private boolean function(SyntaxTree node) {
        if(!match(FN, node)){
            error("Expected 'fn' keyword at the beginning of function declaration.");
            return false;
        }

        if(!match(IDENTIFIER, node)){
            error("Expected function name identifier.");
            return false;
        }

        if (!match(OPEN_PARENTHESIS, node)){
            error("Expected '(' after function name.");
            return false;
        }

        if(!parameterList(node.insertChild(PARAMETER_LIST))) {
            error("Error in parameter list.");
            return false;
        }

        if (!match(CLOSE_PARENTHESIS, node)){
            error("Expected ')' after parameter list.");
            return false;
        }

        if(!match(ARROW, node)){
            error("Expected '->' after parameter list.");
            return false;
        }

        if(!match(TYPE_INT, node)){
            error("Expected return type 'int' after '->'.");
            return false;
        }

        if(!block(node.insertChild(BLOCK))){
            error("Error in function block.");
            return false;
        }

        return true;
    }

    private boolean parameterList(SyntaxTree node) {
        if(check(CLOSE_PARENTHESIS)) {
            node.insertChild(EPSILON);
            return true;
        }

        if (!parameter(node.insertChild(PARAMETER))) {
            return false;
        }

        if(check(COMMA)) {
            match(COMMA, node);
            return parameterList(node);
        }

        return true;
    }

    private boolean parameter(SyntaxTree node) {
        if(!match(IDENTIFIER, node)) {
            error("Expected parameter name identifier.");
            return false;
        }

        if(!match(COLON, node)) {
            error("Expected ':' after parameter name.");
            return false;
        }

        if(!match(TYPE_INT, node)) {
            error("Expected parameter type 'int'.");
            return false;
        }

        return true;
    }

    private boolean block(SyntaxTree node) {
        if(!match(OPEN_BRACE, node)) {
            error("Expected '{' at the beginning of block.");
            return false;
        }

        if(!blockStatements(node)){
            return false;
        }

        if(!match(CLOSE_BRACE, node)) {
            error("Expected '}' at the end of block.");
            return false;
        }

        return true;
    }

    private boolean blockStatements(SyntaxTree node) {
        if(check(CLOSE_BRACE) || check(EOF_TOKEN)) {
            return true;
        }

        if(!statement(node)){
            return false;
        }

        return blockStatements(node);
    }

    private boolean ifStatement(SyntaxTree node) {
        if(!match(IF, node)){
            error("Expected 'if' keyword.");
            return false;
        }

        if(!match(OPEN_PARENTHESIS, node)){
            error("Expected '(' after 'if' keyword.");
            return false;
        }

        if(!condition(node.insertChild(CONDITION))){
            return false;
        }

        if(!match(CLOSE_PARENTHESIS, node)){
            error("Expected ')' after condition.");
            return false;
        }

        if(!block(node.insertChild(BLOCK))){
            error("Expected block after 'if' condition.");
            return false;
        }

        while(check(ELSE_IF)){
            if(!elseIfStatement(node.insertChild(ELSE_IF_STATEMENT))){
                return false;
            }
        }

        if(check(ELSE)){
            if(!elseStatement(node.insertChild(ELSE_STATEMENT))){
                return false;
            }
        }

        return true;
    }

    private boolean elseIfStatement(SyntaxTree node) {
        if(!match(ELSE_IF, node)){
            error("Expected 'elseif' keyword.");
            return false;
        }

        if(!match(OPEN_PARENTHESIS, node)){
            error("Expected '(' after 'elseif' keyword.");
            return false;
        }

        if(!condition(node.insertChild(CONDITION))){
            return false;
        }

        if(!match(CLOSE_PARENTHESIS, node)){
            error("Expected ')' after condition.");
            return false;
        }

        if(!block(node.insertChild(BLOCK))){
            error("Expected block after 'elseif' condition.");
            return false;
        }

        return true;
    }

    private boolean elseStatement(SyntaxTree node) {
        if(!match(ELSE, node)){
            error("Expected 'else' keyword.");
            return false;
        }

        if(!block(node.insertChild(BLOCK))){
            error("Expected block after 'else' keyword.");
            return false;
        }

        return true;
    }

    private boolean condition(SyntaxTree node) {
        if(!expression(node.insertChild(EXPRESSION))) {
            return false;
        }

        if(matchComparisonOperator(node)) {
            if(!expression(node.insertChild(EXPRESSION))) {
                return false;
            }
        } else {
            error("Expected comparison operator in condition.");
            return false;
        }

        return true;
    }

    private boolean matchComparisonOperator(SyntaxTree node) {
        return switch(currentToken.getType()) {
            case EQUAL -> match(EQUAL, node);
            case NOT_EQUAL -> match(NOT_EQUAL, node);
            case GREATER_THAN -> match(GREATER_THAN, node);
            case LESS_THAN -> match(LESS_THAN, node);
            case GREATER_EQUAL -> match(GREATER_EQUAL, node);
            case LESS_EQUAL -> match(LESS_EQUAL, node);
            default -> false; 
        };
    }

    private boolean returnStatement(SyntaxTree node) {
        if(!match(RETURN, node)) {
            error("Expected 'return' keyword.");
            return false;
        }

        if(!expression(node.insertChild(EXPRESSION))) {
            return false;
        }

        return true;
    }

    private boolean expression(SyntaxTree node) {
        if(!term(node.insertChild(TERM))) {
            error("Expected numeric literal in expression.");
            return false;
        }
        
        if(!rightExpression(node.insertChild(RIGHT_EXPRESSION))){
                return false;
        }
        
        return true;
    }

    private boolean rightExpression(SyntaxTree node) {
        if (!check(PLUS) && !check(MINUS)) {
            node.insertChild(EPSILON);
            return true;
        }

        if(check(PLUS)) {
            match(PLUS, node);
        } else if (check(MINUS)) {
            match(MINUS, node);
        }

        if(!term(node.insertChild(TERM))) {
            error("Expected term after '+' or '-'.");
            return false;
        }

        if(!rightExpression(node.insertChild(RIGHT_EXPRESSION))) {
            return false;
        }

        return true;
    }

    private boolean term(SyntaxTree node) {
        if(!factor(node.insertChild(FACTOR))) {
            error("Expected numeric literal in term.");
            return false;
        }

        if(!rightTerm(node.insertChild(RIGHT_TERM))) {
            return false;
        }

        return true;
    }

    private boolean rightTerm(SyntaxTree node) {
        if (!check(MULTIPLY) && !check(DIVIDE)) {
            node.insertChild(EPSILON);
            return true;
        }

        if(check(MULTIPLY)) {
            match(MULTIPLY, node);
        } else if (check(DIVIDE)) {
            match(DIVIDE, node);
        }

        if(!factor(node.insertChild(FACTOR))) {
            error("Expected factor after '*' or '/'.");
            return false;
        }

        if(!rightTerm(node.insertChild(RIGHT_TERM))) {
            return false;
        }

        return true;
    }

    private boolean factor(SyntaxTree node) {
        if(check(OPEN_PARENTHESIS)) {
            match(OPEN_PARENTHESIS, node);

            if(!expression(node.insertChild(EXPRESSION))) {
                return false;
            }

            if(!match(CLOSE_PARENTHESIS, node)) {
                error("Expected ')' after expression.");
                return false;
            }

            return true;
        }

        if (check(NUMERIC)) {
            match(NUMERIC, node);
            return true;
        }

        if(check(IDENTIFIER)) {
            if(lookAhead(OPEN_PARENTHESIS)) {
                return functionCall(node.insertChild(FUNCTION_CALL));
            } else {
                match(IDENTIFIER, node);
                return true;
            }
        }

        error("Expected numeric literal, identifier, or '(' in factor.");
        return false;
    }

    private boolean functionCall(SyntaxTree node) {
        if(!match(IDENTIFIER, node)) {
            error("Expected function name identifier in function call.");
            return false;
        }

        if(!match(OPEN_PARENTHESIS, node)) {
            error("Expected '(' after function name in function call.");
            return false;
        }

        if(!expressionList(node.insertChild(EXPRESSION_LIST))) {
            return false;
        }

        if(!match(CLOSE_PARENTHESIS, node)) {
            error("Expected ')' after expression list in function call.");
            return false;
        }

        return true;
    }

    private boolean expressionList(SyntaxTree node) {
        if(check(CLOSE_PARENTHESIS)) {
            node.insertChild(EPSILON);
            return true;
        }

        if(!expression(node.insertChild(EXPRESSION))) {
            return false;
        }

        if(check(COMMA)) {
            match(COMMA, node);
            return expressionList(node);
        }

        return true;
    }

    private boolean check(byte expectedType) {
        return currentToken.getType() == expectedType;
    }

    private boolean lookAhead(byte expectedType) {
        if(position + 1 >= tokens.size()) {
            return false;
        }
        return tokens.get(position + 1).getType() == expectedType;
    }

    private boolean match(byte expectedType, SyntaxTree node) {
        System.out.printf("Matching token: expected %s, got %s\n", Token.getTokenName(expectedType), currentToken);

        if(!check(expectedType)) {
            return false;
        }

        node.insertChild(currentToken);

        position++;
        if(position < tokens.size()) {
            currentToken = tokens.get(position);
        }
        return true;
    }

    private void error(String message) {
        System.err.printf("Parser error at line %d (position: %d) -- %s\n", currentToken.getLine(), currentToken.getPosition(), message);
    }

}
