package pl.peth.common.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.peth.common.Token;

public class SyntaxTree {
    private final List<SyntaxTree> children;
    private final byte type;
    private String value;
    private final Map<String, String> attributes;

    public SyntaxTree(byte type) {
        this.type = type;
        this.value = null;
        this.children = new ArrayList<>();
        this.attributes = new HashMap<>();
    }

    public SyntaxTree(byte type, String value) {
        this(type);
        this.value = value;
    }
    
    public SyntaxTree addChild(SyntaxTree child) {
        if (child != null) {
            children.add(child);
        }
        return child;
    }

    public SyntaxTree addChild(byte type) {
        SyntaxTree child = new SyntaxTree(type);
        children.add(child);
        return child;
    }

    public SyntaxTree addChild(byte type, String value) {
        SyntaxTree child = new SyntaxTree(type, value);
        children.add(child);
        return child;
    }
    
    public SyntaxTree withAttribute(String key, String value) {
        attributes.put(key, value);
        return this;
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    // === Getters ===
    
    public byte getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public List<SyntaxTree> getChildren() {
        return children;
    }

    public int getChildCount() {
        return children.size();
    }

    public SyntaxTree getChild(int index) {
        if (index < 0 || index >= children.size()) {
            return null;
        }
        return children.get(index);
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }
    
    public void print() {
        print("", false );
    }

    private void print(String prefix, boolean isLast) {
        System.out.print(prefix);
        System.out.print(isLast ? "└── " : "├── ");
        
        System.out.print(Token.getTokenName(type));
        
        if (value != null && !value.isEmpty()) {
            System.out.print("('" + value + "')");
        }
        
        if (!attributes.isEmpty()) {
            System.out.print(" {");
            boolean first = true;
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                if (!first) System.out.print(", ");
                System.out.print(entry.getKey() + "=" + entry.getValue());
                first = false;
            }
            System.out.print("}");
        }
        
        System.out.println();

        for (int i = 0; i < children.size(); i++) {
            children.get(i).print(
                prefix + (isLast ? "    " : "│   "),
                i == children.size() - 1
            );
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Token.getTokenName(type));
        if (value != null) {
            sb.append("('").append(value).append("')");
        }
        return sb.toString();
    }
}