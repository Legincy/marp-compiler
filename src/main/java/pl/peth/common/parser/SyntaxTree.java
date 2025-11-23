package pl.peth.common.parser;

import java.util.ArrayList;
import java.util.List;

import pl.peth.common.Token;

public class SyntaxTree {
    private final List<SyntaxTree> childNodes;
    private final Token token;
    private final byte type;

    public SyntaxTree(byte type) {
        this.type = type;
        this.token = null;
        this.childNodes = new ArrayList<>();
    }

    public SyntaxTree(Token token) {
        this.type = token.getType();
        this.token = token;
        this.childNodes = new ArrayList<>();
    }

    public void addChild(SyntaxTree child){
        if (child != null) {
            this.childNodes.add(child);
        }
    }

    public SyntaxTree insertChild(byte type) {
        SyntaxTree child = new SyntaxTree(type);
        childNodes.add(child);
        return child;
    }

    public SyntaxTree insertChild(Token token) {
        SyntaxTree child = new SyntaxTree(token);
        childNodes.add(child);
        return child;
    }

    public byte getType() {
        return type;
    }

    public Token getToken() {
        return token;
    }

    public List<SyntaxTree> getChildNodes() {
        return childNodes;
    }

    public int getChildCount() {
        return childNodes.size();
    }

    public SyntaxTree getChild(int index) {
        if (index < 0 || index >= childNodes.size()) {
            return null;
        }
        return childNodes.get(index);
    }

    public String getLexeme() {
        if (token != null) {
            return token.getLexeme();
        }
        return null;
    }

    public boolean isTerminal() {
        return token != null;
    }

    public boolean isNonTerminal() {
        return token == null;
    }

    public void print() {
        print(0);
    }

    private void print(int depth) {
        for (int i = 0; i < depth; i++) {
            System.out.print("│   ");
        }

        if(depth > 0){
            System.out.print("├── ");
        }

        System.out.print(Token.getTokenName(getType()));

        if(token != null && !token.getLexeme().isEmpty()){
            System.out.printf("('%s')", token.getLexeme());
        }

        System.out.println();

        for (SyntaxTree child : childNodes) {
            child.print(depth + 1);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb, 0);
        return sb.toString();
    }

    private void toString(StringBuilder sb, int depth) {
        sb.append("  ".repeat(Math.max(0, depth)));
        sb.append(token.getTokenName());
        if (token != null && !token.getLexeme().isEmpty()) {
            sb.append("('").append(token.getLexeme()).append("')");
        }
        sb.append("\n");

        for (SyntaxTree child : childNodes) {
            child.toString(sb, depth + 1);
        }
    }
}
