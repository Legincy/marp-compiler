package pl.peth.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.peth.common.parser.SyntaxTree;
import pl.peth.common.tokens.ITokenWrapper;

public class CodeGenerator implements ITokenWrapper {
    private final List<Instruction> instructions;
    private final Map<String, Integer> functionTable;
    private final Map<String, Integer> localVariableOffsets;
    private final Map<String, Integer> globalVariableOffsets;

    private int currentOffset;
    private int labelCounter;
    private String currentFunction;
    private int currentParameterCounter;
    private int localVariableCounter;
    private int globalVariableCounter;
    private boolean inScope;

    public CodeGenerator() {
        this.instructions = new ArrayList<>();
        this.functionTable = new HashMap<>();
        this.localVariableOffsets = new HashMap<>();
        this.globalVariableOffsets = new HashMap<>();
        this.currentOffset = 0;
        this.labelCounter = 0;
        this.currentFunction = null;
        this.currentParameterCounter = 0;
        this.localVariableCounter = 0;
        this.globalVariableCounter = 0;
        this.inScope = false;
    }

    public List<Instruction> generate(SyntaxTree syntaxTree) {
        instructions.clear();
        functionTable.clear();
        globalVariableOffsets.clear();
        globalVariableCounter = 0;

        collectGlobalVariables(syntaxTree);

        if(globalVariableCounter > 0) {
            emit(OperationCode.ENTER, globalVariableCounter).withComment("enter::global_variables::" + globalVariableCounter);
        }

        generateGlobalInitializations(syntaxTree);

        emit(OperationCode.CALL, 0).withComment("call::main");
        int mainCallIndex = instructions.size() - 1;
        emit(OperationCode.HALT).withComment("call::halt");

        generateNode(syntaxTree);

        if(functionTable.containsKey("main")) {
            instructions.set(mainCallIndex, new Instruction(OperationCode.CALL, functionTable.get("main")).withComment("call::main"));
        }

        return instructions;
    }

    private void collectGlobalVariables(SyntaxTree node) {
        if (node == null) return;

        if(node.getType() == PROGRAM) {
            for(SyntaxTree child: node.getChildren()){
                if(child.getType() == VARIABLE_DECLARATION) {
                    String globalVariableName = child.getAttribute("name");
                    if(globalVariableName != null && !globalVariableOffsets.containsKey(globalVariableName)) {
                        globalVariableOffsets.put(globalVariableName, globalVariableCounter);
                        globalVariableCounter++;
                    }
                }
            }
        } 
    }

    public void generateGlobalInitializations(SyntaxTree node) {
        if(node == null || node.getType() != PROGRAM) return;

        for(SyntaxTree child: node.getChildren()){
            if(child.getType() == VARIABLE_DECLARATION) {
                String globalVariableName = child.getAttribute("name");
                int offset = globalVariableOffsets.get(globalVariableName);

                if(child.getChildCount() > 0) {
                    generateNode(child.getChild(0));
                } else {
                    emit(OperationCode.PUSH, 0).withComment("global::default::" + globalVariableName);
                }
                emit(OperationCode.GSTORE, offset).withComment("gstore::init::" + globalVariableName);
            }
        }
    }

    public void generateNode(SyntaxTree node) {
        if (node == null) return;

        byte type = node.getType();

        switch(type) {
            case PROGRAM -> generateProgram(node);
            case FUNCTION -> generateFunction(node);
            case ASSIGNMENT -> generateAssignment(node);
            case RETURN -> generateReturn(node);
            case IF -> generateIf(node);
            case WHILE -> generateWhile(node);
            case EXPRESSION -> generateExpression(node);
            case TERM -> generateTerm(node);
            case FACTOR -> generateFactor(node);
            case FUNCTION_CALL -> generateFunctionCall(node);
            case CONDITION -> generateCondition(node);
            case NUMERIC -> {
                int value = Integer.parseInt(node.getValue());
                emit(OperationCode.PUSH, value).withComment("push::numeric::" + value);
            }
            case IDENTIFIER -> {
                String identifierName = node.getValue();
                if(localVariableOffsets.containsKey(identifierName)) {
                    int offset = localVariableOffsets.get(identifierName);
                    emit(OperationCode.LOAD, offset).withComment("load::local::" + identifierName);
                } else if(globalVariableOffsets.containsKey(identifierName)) {
                    int offset = globalVariableOffsets.get(identifierName);
                    emit(OperationCode.GLOAD, offset).withComment("gload::global::" + identifierName);
                } else {
                    error("Undefined identifier: " + identifierName);
                    emit(OperationCode.PUSH, 0).withComment("undefined::" + identifierName);
                }
            }
            case VARIABLE_DECLARATION -> {
                if(this.inScope) {
                    generateVariableDeclaration(node);
                }
            }
            default -> {
                for(SyntaxTree child: node.getChildren()){
                    generateNode(child);
                }
            }
        }
    }

    private void generateProgram(SyntaxTree node) {
        for(SyntaxTree child: node.getChildren()){
            if (child.getType() != VARIABLE_DECLARATION) {
                generateNode(child);
            }
        }

    }

    private void generateFunction(SyntaxTree node) {
        String functionName = node.getAttribute("name");
        SyntaxTree parameterNodes = null;
        SyntaxTree blockNode = null;
    
        for(SyntaxTree child: node.getChildren()){
            if(child.getType() == PARAMETER_LIST) {
                parameterNodes = child;
            } else if(child.getType() == BLOCK) {
                blockNode = child;
            }
        }

        if(functionName == null || blockNode == null) {
            error("Function node is missing name or block.");
            return;
        }

        this.currentFunction = functionName;
        this.functionTable.put(functionName, this.instructions.size());
        this.inScope = true;
        
        this.localVariableOffsets.clear();
        this.currentOffset = 0;
        this.localVariableCounter = 0;

        List<String> functionParameters = new ArrayList<>();
        if (parameterNodes != null) {
            extractParameters(parameterNodes, functionParameters);
        }
     
        int parameterCount = functionParameters.size();
        this.currentParameterCounter = parameterCount;
        for(int i = 0; i < parameterCount; i++) {
            String parameterName = functionParameters.get(i);
            int offset = -(parameterCount - i + 2);
            this.localVariableOffsets.put(parameterName, offset);
        }

        countLocalVariables(blockNode);

        emit(OperationCode.ENTER, this.localVariableCounter).withLabel(functionName).withComment(String.format("enter::%s::locals::%d", functionName, this.localVariableCounter));
        
        generateBlock(blockNode);

        emit(OperationCode.PUSH, 0).withComment("push::default_return_value");
        emit(OperationCode.RET, currentParameterCounter).withComment("exit::"+functionName);

        this.inScope = false;
        currentFunction = null;
        currentParameterCounter = 0;

    }

    private void countLocalVariables(SyntaxTree node) {
        if (node == null) return;
        
        if (node.getType() == VARIABLE_DECLARATION) {
            this.localVariableCounter++;
        }
        
        for (SyntaxTree child : node.getChildren()) {
            countLocalVariables(child);
        }
    }

    private void extractParameters(SyntaxTree node, List<String> parameters) {
        for(SyntaxTree parameterChild : node.getChildren()) {
            if(parameterChild.getType() == PARAMETER) {
                String parameterName = parameterChild.getAttribute("name");
                if(parameterName != null) {
                    parameters.add(parameterName);
                }
            }
        }
    }

    private void generateVariableDeclaration(SyntaxTree node) {
        String variableName = node.getAttribute("name");
        
        if(variableName == null) {
            error("Variable declaration missing name.");
            return;
        }

        int offset = this.currentOffset;
        this.localVariableOffsets.put(variableName, offset);
        currentOffset++;

        if(node.getChildCount() > 0) {
            generateNode(node.getChild(0));
            emit(OperationCode.STORE, offset).withComment("store::init::" + variableName);
        } else {
            emit(OperationCode.PUSH, 0).withComment("variable::default::" + variableName);
            emit(OperationCode.STORE, offset).withComment("store::default::" + variableName);
        }
    }

    private void generateAssignment(SyntaxTree node) {
        String variableName = node.getAttribute("name");
        
        if(variableName == null){
            error("Assigntment missing variable name.");
            return;
        }

        if(node.getChildCount() > 0){
            generateNode(node.getChild(0));
        } else {
            emit(OperationCode.PUSH, 0);
        }

        if(localVariableOffsets.containsKey(variableName)) {
            int offset = localVariableOffsets.get(variableName);
            emit(OperationCode.STORE, offset).withComment("store::local::" + variableName);
        } else if(globalVariableOffsets.containsKey(variableName)) {
            int offset = globalVariableOffsets.get(variableName);
            emit(OperationCode.GSTORE, offset).withComment("gstore::global::" + variableName);
        } else {
            error("Undefined variable in assigntment: " + variableName);
        }
    }

    private void generateBlock(SyntaxTree node) {
        for(SyntaxTree child: node.getChildren()){
            byte type = child.getType();
            if(type != OPEN_BRACE && type != CLOSE_BRACE) {
                generateNode(child);
            }
        }
    }

    private void generateReturn(SyntaxTree node) {
       if(node.getChildCount() > 0){
            generateNode(node.getChild(0));
        } else {
            emit(OperationCode.PUSH, 0).withComment("return::default_value");
        }

        emit(OperationCode.RET, currentParameterCounter).withComment("return::" + currentFunction);
    }

    private void generateIf(SyntaxTree node) {
        List<SyntaxTree> children = node.getChildren();

        SyntaxTree conditionNode = null;
        SyntaxTree thenBlockNode = null;
        List<SyntaxTree> elseIfNodes = new ArrayList<>();
        SyntaxTree elseNode = null;

        for(SyntaxTree child: children){
            byte type = child.getType();

            if(type == CONDITION && conditionNode == null) {
                conditionNode = child;
            } else if(type == BLOCK && thenBlockNode == null) {
                thenBlockNode = child;
            } else if(type == ELSE_IF) {
                elseIfNodes.add(child);
            } else if(type == ELSE) {
                elseNode = child;
            }
        }

        if (conditionNode == null || thenBlockNode == null) {
            error("If statement missing condition or then block.");
            return;
        }

        List<Integer> endJumps = new ArrayList<>();

        generateCondition(conditionNode);
        int jumpToElseIndex = this.instructions.size();
        emit(OperationCode.JZ, 0).withComment("if_false::jump_to_else");

        generateBlock(thenBlockNode);
        endJumps.add(instructions.size());
        emit(OperationCode.JMP, 0).withComment("if_end::jump_to_end");

        patchJump(jumpToElseIndex, instructions.size());

        for (SyntaxTree elseIfNode : elseIfNodes) {
            SyntaxTree elseIfCondition = null;
            SyntaxTree elseIfBlock = null;
            
            for (SyntaxTree child : elseIfNode.getChildren()) {
                if (child.getType() == CONDITION) {
                    elseIfCondition = child;
                } else if (child.getType() == BLOCK) {
                    elseIfBlock = child;
                }
            }

            if (elseIfCondition != null) {
                generateCondition(elseIfCondition);
                int elseIfJumpIndex = instructions.size();
                emit(OperationCode.JZ, 0).withComment("elseif_false::jump_to_next");

                if (elseIfBlock != null) {
                    generateBlock(elseIfBlock);
                }
                endJumps.add(instructions.size());
                emit(OperationCode.JMP, 0).withComment("elseif_end::jump_to_end");

                patchJump(elseIfJumpIndex, instructions.size());
            }
        }

        if (elseNode != null) {
            for (SyntaxTree child : elseNode.getChildren()) {
                if (child.getType() == BLOCK) {
                    generateBlock(child);
                    break;
                }
            }
        }

        int endAddress = instructions.size();
        for (int jumpIndex : endJumps) {
            patchJump(jumpIndex, endAddress);
        }
    }

    private void generateWhile(SyntaxTree node) {
        List<SyntaxTree> children = node.getChildren();

        int loopStart = this.instructions.size();

        SyntaxTree conditionNode = null;
        SyntaxTree blockNode = null;

        for(SyntaxTree child: children){
            byte type = child.getType();

            if(type == CONDITION && conditionNode == null) {
                conditionNode = child;
            } else if(type == BLOCK && blockNode == null) {
                blockNode = child;
            }
        }

        if (conditionNode == null || blockNode == null) {
            error("While statement missing condition or block.");
            return;
        }

        generateCondition(conditionNode);
        int exitJumpIndex = this.instructions.size();
        emit(OperationCode.JZ, 0).withComment("while_exit::jump");

        generateBlock(blockNode);
        emit(OperationCode.JMP, loopStart).withComment("while_loop::jump_back");

        patchJump(exitJumpIndex, instructions.size());
    }

    private void generateElseIfStatement(SyntaxTree node, List<Integer> endJumps) {
        List<SyntaxTree> children = node.getChildren();

        int i = 0;
        while(i < children.size() && children.get(i).getType() != CONDITION){
            i++;
        }

        if(i >= children.size()) {
            error("Else-if statement missing condition.");
            return;
        }

        generateCondition(children.get(i));
        int jumpToNextIndex = this.instructions.size();
        emit(OperationCode.JZ, 0).withComment("elseif_false::jump_to_next");
        i++;

        while(i < children.size() && children.get(i).getType() != BLOCK){
            i++;
        }

        if(i < children.size()) {
            generateBlock(children.get(i));
        }

        int jumpToEndIndex = this.instructions.size();
        emit(OperationCode.JMP, 0).withComment("elseif_end::jump_to_end");
        endJumps.add(jumpToEndIndex);

        patchJump(jumpToEndIndex, instructions.size());
    }

    private void generateElseStatement(SyntaxTree node) {
        for(SyntaxTree child: node.getChildren()){
            if(child.getType() == BLOCK) {
                generateBlock(child);
                break;
            }
        }
    }

    private void generateCondition(SyntaxTree node) {
        List<SyntaxTree> children = node.getChildren();
        String operator = node.getAttribute("operator");
       
        if (children.size() < 2) {
            error("Condition requires two operands");
            return;
        }

        generateNode(children.get(0));
        generateNode(children.get(1));

         if (operator != null) {
            switch (operator) {
                case "==" -> emit(OperationCode.CMP_EQ).withComment("condition::equals");
                case "!=" -> emit(OperationCode.CMP_NEQ).withComment("condition::not_equals");
                case "<" -> emit(OperationCode.CMP_LT).withComment("condition::less_than");
                case "<=" -> emit(OperationCode.CMP_LTE).withComment("condition::less_equal");
                case ">" -> emit(OperationCode.CMP_GT).withComment("condition::greater_than");
                case ">=" -> emit(OperationCode.CMP_GTE).withComment("condition::greater_equal");
                default -> error("Unknown comparison operator: " + operator);
            }
        }
    }

    private void generateExpression(SyntaxTree node) {
        List<SyntaxTree> children = node.getChildren();
        String operator = node.getAttribute("operator");
        
        if (children.size() >= 2 && operator != null) {
            generateNode(children.get(0));
            generateNode(children.get(1));
            
            switch (operator) {
                case "+" -> emit(OperationCode.ADD).withComment("expression::add");
                case "-" -> emit(OperationCode.SUB).withComment("expression::sub");
                default -> error("Unknown expression operator: " + operator);
            }
        } else {
            for (SyntaxTree child : children) {
                generateNode(child);
            }
        }
    }

    private void generateRightExpression(SyntaxTree node) {
        List<SyntaxTree> children = node.getChildren();
        OperationCode pendingOperation = null;

        for(SyntaxTree child: children){
            byte type = child.getType();

           if(type == PLUS) {
                pendingOperation = OperationCode.ADD;
            } else if(type == MINUS) {
                pendingOperation = OperationCode.SUB;
            } else if(type == TERM) {
                generateTerm(child);
                if(pendingOperation != null) {
                    emit(pendingOperation).withComment("expression::" + pendingOperation.name().toLowerCase());
                    pendingOperation = null;
                }
            } else if (type == RIGHT_EXPRESSION){
                generateRightExpression(child);
            }
        }
    }

    private void generateTerm(SyntaxTree node) {
        List<SyntaxTree> children = node.getChildren();
        String operator = node.getAttribute("operator");


        if (children.size() >= 2 && operator != null) {
            generateNode(children.get(0));
            generateNode(children.get(1));
            
            switch (operator) {
                case "*" -> emit(OperationCode.MUL).withComment("term::mul");
                case "/" -> emit(OperationCode.DIV).withComment("term::div");
                default -> error("Unknown term operator: " + operator);
            }
        } else {
            for (SyntaxTree child : children) {
                generateNode(child);
            }
        }
    }

    private void generateRightTerm(SyntaxTree node) {
        List<SyntaxTree> children = node.getChildren();
        OperationCode pendingOperation = null;

        for(SyntaxTree child: children){
            byte type = child.getType();

           if(type == MULTIPLY) {
                pendingOperation = OperationCode.MUL;
            } else if(type == DIVIDE) {
                pendingOperation = OperationCode.DIV;
            } else if(type == FACTOR) {
                generateFactor(child);
                if(pendingOperation != null) {
                    emit(pendingOperation).withComment("term::" + pendingOperation.name().toLowerCase());
                    pendingOperation = null;
                }
            } else if (type == RIGHT_TERM){
                generateRightTerm(child);
            }
        }
    }

    private void generateFactor(SyntaxTree node) {
        for (SyntaxTree child : node.getChildren()) {
            generateNode(child);
        }
    }

    private void generateFunctionCall(SyntaxTree node) {
        String functionName = node.getAttribute("name");

        if (functionName == null) {
            error("Function call missing function name.");
            return;
        }

        int argumentCount = 0;
        for (SyntaxTree child : node.getChildren()) {
            generateNode(child);
            argumentCount++;
        }

        int callAddress = functionTable.getOrDefault(functionName, 0);
        emit(OperationCode.CALL, callAddress).withComment("call::" + functionName + "::args::" + argumentCount);
    }

    private Instruction emit(OperationCode opCode) {
        Instruction instruction = new Instruction(opCode);
        instructions.add(instruction);
        return instruction;
    }
    
    private Instruction emit(OperationCode opCode, int operand) {
        Instruction instruction = new Instruction(opCode, operand);
        instructions.add(instruction);
        return instruction;
    }

    private String newLabel(String prefix) {
        return prefix + "_" + (labelCounter++);
    }

    private void patchJump(int instructionIndex, int targetAddress) {
        Instruction old = instructions.get(instructionIndex);
        Instruction patched = new Instruction(old.getOpCode(), targetAddress).withComment(old.getComment());
        instructions.set(instructionIndex, patched);
    }

    public void printCode() {
        System.out.println("Generated Instructions:");
        for(int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            System.out.printf("%04d: %s%n", i, instruction.toByteString(i));
        }
        System.out.println("--------------------------");
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public Map<String, Integer> getFunctionTable() {
        return functionTable;
    }

    public int getGlobalVariableCounter() {
        return globalVariableCounter;
    }

    private void error(String message) {
        System.err.println("CodeGenerator Error: " + message);
    }
}