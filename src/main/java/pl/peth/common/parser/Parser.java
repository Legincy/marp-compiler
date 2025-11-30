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

        if (this.tokens == null || this.tokens.isEmpty()) {
            error("No tokens to parse.");
            return null;
        }

        this.currentToken = this.tokens.get(0);
        SyntaxTree program = new SyntaxTree(PROGRAM);

        if (!parseProgram(program)) {
            return null;
        }

        if (currentToken.getType() != EOF_TOKEN) {
            error("Unexpected token after program end: " + currentToken);
            return null;
        }

        return program;
    }

    private boolean parseProgram(SyntaxTree node) {
        while (!check(EOF_TOKEN)) {
            if (!parseStatement(node)) {
                return false;
            }
        }
        return true;
    }

    private boolean parseStatement(SyntaxTree parent) {
        if (check(FN)) {
            return parseFunction(parent);
        }

        if (check(VAR)) {
            return parseVariableDeclaration(parent);
        }

        if (check(IF)) {
            return parseIfStatement(parent);
        }
        if (check(WHILE)) {
            return parseWhileStatement(parent);
        }
        if (check(RETURN)) {
            return parseReturnStatement(parent);
        }

        if (check(IDENTIFIER) && lookAhead(1) == ASSIGN) {
            return parseAssignment(parent);
        }

        SyntaxTree expr = parseExpression();
        if (expr == null) return false;
        parent.addChild(expr);
        return true;
    }

    private boolean parseVariableDeclaration(SyntaxTree parent) {
        if (!expect(VAR)) return false;

        if (!check(IDENTIFIER)) {
            error("Expected variable name");
            return false;
        }

        String variableName = currentToken.getLexeme();
        advance();

        if (!expect(COLON)) return false;

        if (!checkType()) {
            error("Expected variable type");
            return false;
        }

        String variableType = currentToken.getLexeme();
        advance();

        SyntaxTree node = parent.addChild(VARIABLE_DECLARATION)
            .withAttribute("name", variableName)
            .withAttribute("type", variableType);

        if (check(ASSIGN)) {
            advance();
            SyntaxTree expr = parseExpression();
            if (expr == null) return false;
            node.addChild(expr);
        }

        return true;
    }

    private boolean parseAssignment(SyntaxTree parent) {
        if (!check(IDENTIFIER)) {
            error("Expected variable name for assignment");
            return false;
        }

        String variableName = currentToken.getLexeme();
        advance();

        if (!expect(ASSIGN)) return false;

        SyntaxTree assignNode = parent.addChild(ASSIGNMENT).withAttribute("name", variableName);
        SyntaxTree expressionNode = parseExpression();

        if(expressionNode == null) return false;
        assignNode.addChild(expressionNode);

        return true;
    }

    private boolean parseFunction(SyntaxTree parent) {
        if (!expect(FN)) return false;

        if (!check(IDENTIFIER)) {
            error("Expected function name");
            return false;
        }
        String functionName = currentToken.getLexeme();
        advance();

        SyntaxTree funcNode = parent.addChild(FUNCTION)
            .withAttribute("name", functionName);

        if (!expect(OPEN_PARENTHESIS)) return false;

        SyntaxTree params = funcNode.addChild(PARAMETER_LIST);
        if (!parseParameterList(params)) return false;

        if (!expect(CLOSE_PARENTHESIS)) return false;
        if (!expect(ARROW)) return false;

        if (!checkType()) {
            error("Expected return type");
            return false;
        }
        funcNode.withAttribute("returnType", currentToken.getLexeme());
        advance();

        SyntaxTree body = funcNode.addChild(BLOCK);
        if (!parseBlock(body)) return false;

        return true;
    }

    private boolean parseParameterList(SyntaxTree node) {
        if (check(CLOSE_PARENTHESIS)) {
            return true;
        }

        if (!parseParameter(node)) return false;

        while (check(COMMA)) {
            advance();
            if (!parseParameter(node)) return false;
        }

        return true;
    }

    private boolean parseParameter(SyntaxTree parent) {
        if (!check(IDENTIFIER)) {
            error("Expected parameter name");
            return false;
        }
        String paramName = currentToken.getLexeme();
        advance();

        if (!expect(COLON)) return false;

        if (!checkType()) {
            error("Expected parameter type");
            return false;
        }
        String paramType = currentToken.getLexeme();
        advance();

        parent.addChild(PARAMETER)
            .withAttribute("name", paramName)
            .withAttribute("type", paramType);

        return true;
    }

    private boolean parseBlock(SyntaxTree node) {
        if (!expect(OPEN_BRACE)) return false;

        while (!check(CLOSE_BRACE) && !check(EOF_TOKEN)) {
            if (!parseStatement(node)) return false;
        }

        if (!expect(CLOSE_BRACE)) return false;
        return true;
    }

    private boolean parseIfStatement(SyntaxTree parent) {
        if (!expect(IF)) return false;

        SyntaxTree ifNode = parent.addChild(IF);

        if (!expect(OPEN_PARENTHESIS)) return false;
        
        SyntaxTree condition = parseCondition();
        if (condition == null) return false;
        ifNode.addChild(condition);

        if (!expect(CLOSE_PARENTHESIS)) return false;

        SyntaxTree thenBlock = ifNode.addChild(BLOCK);
        if (!parseBlock(thenBlock)) return false;

        while (check(ELSE_IF)) {
            advance();
            SyntaxTree elseIfNode = ifNode.addChild(ELSE_IF);

            if (!expect(OPEN_PARENTHESIS)) return false;
            
            SyntaxTree elseIfCondition = parseCondition();
            if (elseIfCondition == null) return false;
            elseIfNode.addChild(elseIfCondition);

            if (!expect(CLOSE_PARENTHESIS)) return false;

            SyntaxTree elseIfBlock = elseIfNode.addChild(BLOCK);
            if (!parseBlock(elseIfBlock)) return false;
        }

        if (check(ELSE)) {
            advance();
            SyntaxTree elseBlock = ifNode.addChild(ELSE);
            if (!parseBlock(elseBlock)) return false;
        }

        return true;
    }

    private boolean parseWhileStatement(SyntaxTree parent) {
        if (!expect(WHILE)) return false;

        SyntaxTree whileNode = parent.addChild(WHILE);

        if (!expect(OPEN_PARENTHESIS)) return false;

        SyntaxTree condition = parseCondition();
        if (condition == null) return false;
        whileNode.addChild(condition);

        if (!expect(CLOSE_PARENTHESIS)) return false;

        SyntaxTree body = whileNode.addChild(BLOCK);
        if (!parseBlock(body)) return false;

        return true;
    }

    private boolean parseReturnStatement(SyntaxTree parent) {
        if (!expect(RETURN)) return false;

        SyntaxTree returnNode = parent.addChild(RETURN);

        SyntaxTree expr = parseExpression();
        if (expr == null) return false;
        returnNode.addChild(expr);

        return true;
    }

    private SyntaxTree parseCondition() {
        SyntaxTree left = parseExpression();
        if (left == null) return null;

        if (!isComparisonOperator()) {
            error("Expected comparison operator in condition");
            return null;
        }

        byte opType = currentToken.getType();
        String opSymbol = currentToken.getLexeme();
        advance();

        SyntaxTree right = parseExpression();
        if (right == null) return null;

        SyntaxTree condition = new SyntaxTree(CONDITION)
            .withAttribute("operator", opSymbol);
        condition.addChild(left);
        condition.addChild(right);

        return condition;
    }

    private SyntaxTree parseExpression() {
        SyntaxTree left = parseTerm();
        if (left == null) return null;

        while (check(PLUS) || check(MINUS)) {
            String op = currentToken.getLexeme();
            advance();

            SyntaxTree right = parseTerm();
            if (right == null) return null;

            SyntaxTree binOp = new SyntaxTree(EXPRESSION)
                .withAttribute("operator", op);
            binOp.addChild(left);
            binOp.addChild(right);
            left = binOp;
        }

        return left;
    }

    private SyntaxTree parseTerm() {
        SyntaxTree left = parseFactor();
        if (left == null) return null;

        while (check(MULTIPLY) || check(DIVIDE)) {
            String op = currentToken.getLexeme();
            advance();

            SyntaxTree right = parseFactor();
            if (right == null) return null;

            SyntaxTree binOp = new SyntaxTree(TERM)
                .withAttribute("operator", op);
            binOp.addChild(left);
            binOp.addChild(right);
            left = binOp;
        }

        return left;
    }

    private SyntaxTree parseFactor() {
        if (check(OPEN_PARENTHESIS)) {
            advance();
            SyntaxTree expr = parseExpression();
            if (expr == null) return null;
            if (!expect(CLOSE_PARENTHESIS)) return null;
            return expr;
        }

        if (check(NUMERIC)) {
            SyntaxTree numNode = new SyntaxTree(NUMERIC, currentToken.getLexeme());
            advance();
            return numNode;
        }

        if (check(IDENTIFIER)) {
            String name = currentToken.getLexeme();
            advance();

            if (check(OPEN_PARENTHESIS)) {
                return parseFunctionCall(name);
            } else {
                return new SyntaxTree(IDENTIFIER, name);
            }
        }

        error("Expected expression");
        return null;
    }

    private SyntaxTree parseFunctionCall(String functionName) {
        SyntaxTree callNode = new SyntaxTree(FUNCTION_CALL)
            .withAttribute("name", functionName);

        if (!expect(OPEN_PARENTHESIS)) return null;

        if (!check(CLOSE_PARENTHESIS)) {
            SyntaxTree arg = parseExpression();
            if (arg == null) return null;
            callNode.addChild(arg);

            while (check(COMMA)) {
                advance();
                arg = parseExpression();
                if (arg == null) return null;
                callNode.addChild(arg);
            }
        }

        if (!expect(CLOSE_PARENTHESIS)) return null;
        return callNode;
    }

    private boolean check(byte type) {
        return currentToken.getType() == type;
    }

    private boolean checkType() {
        return check(TYPE_INT) || check(TYPE_STRING) || check(TYPE_BOOL) || check(TYPE_VOID);
    }

    private boolean isComparisonOperator() {
        byte t = currentToken.getType();
        return t == EQUAL || t == NOT_EQUAL || 
               t == GREATER_THAN || t == LESS_THAN || 
               t == GREATER_EQUAL || t == LESS_EQUAL;
    }

    private byte lookAhead(int offset) {
        int index = position + offset;
        if (index >= tokens.size()) {
            return EOF_TOKEN;
        }
        return tokens.get(index).getType();
    }

    private void advance() {
        position++;
        if (position < tokens.size()) {
            currentToken = tokens.get(position);
        }
    }

    private boolean expect(byte type) {
        if (!check(type)) {
            error("Expected " + Token.getTokenName(type) + " but got " + currentToken.getTokenName());
            return false;
        }
        advance();
        return true;
    }

    private void error(String message) {
        System.err.printf("Parser error at [line: %d, position: %d] - %s%n",
            currentToken.getLine(), currentToken.getPosition(), message);
    }
}