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
    private final Map<String, Integer> variableOffsets;
    private final List<Integer> patchList;

    private int currentOffset;
    private int labelCounter;
    private String currentFunction;
    private int currentParameterCounter;

    public CodeGenerator() {
        this.instructions = new ArrayList<>();
        this.functionTable = new HashMap<>();
        this.variableOffsets = new HashMap<>();
        this.patchList = new ArrayList<>();
        this.currentOffset = 0;
        this.labelCounter = 0;
        this.currentFunction = null;
        this.currentParameterCounter = 0;
    }

    public List<Instruction> generate(SyntaxTree syntaxTree) {
        instructions.clear();
        functionTable.clear();

        emit(OperationCode.CALL, 0).withComment("call::main");
        int mainCallIndex = instructions.size() - 1;
        emit(OperationCode.HALT).withComment("call::halt");

        generateNode(syntaxTree);

        if(functionTable.containsKey("main")) {
            instructions.set(mainCallIndex, new Instruction(OperationCode.CALL, functionTable.get("main")).withComment("call::main"));
        }

        return instructions;
    }

    public void generateNode(SyntaxTree node) {
        if (node == null) return;

        byte type = node.getType();

        switch(type) {
            case PROGRAM -> generateProgram(node);
            case FUNCTION -> generateFunction(node);
            case RETURN -> generateReturnStatement(node);
            case IF -> generateIfStatement(node);
            case EXPRESSION -> generateExpression(node);
            case TERM -> generateTerm(node);
            case FACTOR -> generateFactor(node);
            case FUNCTION_CALL -> generateFunctionCall(node);
            case CONDITION -> generateCondition(node);
            default -> {
                for(SyntaxTree child: node.getChildren()){
                    generateNode(child);
                }
            }
        }

    }

    private void generateProgram(SyntaxTree node) {
        for(SyntaxTree child: node.getChildren()){
            generateNode(child);
        }
    }

    private void generateFunction(SyntaxTree node) {
        String functionName = null;
        List<String> functionParameters = new ArrayList<>();
        SyntaxTree blockNode = null;
    
        for(SyntaxTree child: node.getChildren()){
            if(child.getType() == IDENTIFIER && functionName == null) {
                functionName = child.getValue();
            } else if(child.getType() == PARAMETER_LIST) {
                extractParameters(child, functionParameters);
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
        
        this.variableOffsets.clear();
        this.currentOffset = 0;
     
        int parameterCount = functionParameters.size();
        this.currentParameterCounter = parameterCount;
        for(int i = 0; i < parameterCount; i++) {
            String parameterName = functionParameters.get(i);
            int offset = -(parameterCount - i + 2);
            this.variableOffsets.put(parameterName, offset);
        }

        emit(OperationCode.ENTER, 0).withLabel(functionName).withComment("enter::" + functionName);
        
        generateBlock(blockNode);

        emit(OperationCode.PUSH, 0).withComment("push::default_return_value");
        emit(OperationCode.RET, currentParameterCounter).withComment("exit::"+functionName);

        currentFunction = null;
        currentParameterCounter = 0;

    }

    private void extractParameters(SyntaxTree node, List<String> parameters) {
        for(SyntaxTree parameterChild : node.getChildren()) {
            if(parameterChild.getType() == PARAMETER) {
               for(SyntaxTree child : parameterChild.getChildren()) {
                    if(child.getType() == IDENTIFIER) {
                        parameters.add(child.getValue());
                        break;
                    }
                }
            }
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

    private void generateReturnStatement(SyntaxTree node) {
        for(SyntaxTree child: node.getChildren()){
            if(child.getType() == EXPRESSION) {
                generateExpression(child);
                break;
            }
        }
        emit(OperationCode.RET, currentParameterCounter).withComment("return::"+currentFunction);
    }

    private void generateIfStatement(SyntaxTree node) {
        List<SyntaxTree> children = node.getChildren();

        String endLabel = newLabel("if_end");
        String nextLabel = newLabel("if_next");

        int i = 0;
        while(i < children.size() && children.get(i).getType() != CONDITION){
            i++;
        }

        if(i >= children.size()) {
            error("If statement missing condition.");
            return;
        }

        generateCondition(children.get(i));
        int jumpToNextIndex = this.instructions.size();
        emit(OperationCode.JZ, 0).withComment("if_false::jump_to_next");
        i++;

        while(i < children.size() && children.get(i).getType() != BLOCK){
            i++;
        }

        if(i < children.size()) {
            generateBlock(children.get(i));
            i++;
        }

        int jumpToEndIndex = this.instructions.size();
        emit(OperationCode.JMP, 0).withComment("if_end::jump_to_end");

        patchJump(jumpToEndIndex, instructions.size());

        List<Integer> endJumps = new ArrayList<>();
        endJumps.add(jumpToNextIndex);

        while(i < children.size()) {
            SyntaxTree child = children.get(i);

            if(child.getType() == ELSE_IF) {
                generateElseIfStatement(child, endJumps);
            } else if(child.getType() == ELSE) {
                generateElseStatement(node);
            }
            i++;
        }

        int endAddress = instructions.size();
        for(int jumpIndex : endJumps) {
            patchJump(jumpIndex, endAddress);
        }
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

        if(children.size() >= 1){
            generateExpression(children.get(0));
        }

        if(children.size() >= 3){
            SyntaxTree operationNode = children.get(1);
            generateExpression(children.get(2));

            byte operationType = operationNode.getType();
            switch(operationType) {
                case EQUAL -> emit(OperationCode.CMP_EQ).withComment("condition::equals");
                case NOT_EQUAL -> emit(OperationCode.CMP_NEQ).withComment("condition::not_equals");
                case LESS_THAN -> emit(OperationCode.CMP_LT).withComment("condition::less_than");
                case LESS_EQUAL -> emit(OperationCode.CMP_LTE).withComment("condition::less_equal");
                case GREATER_THAN -> emit(OperationCode.CMP_GT).withComment("condition::greater_than");
                case GREATER_EQUAL -> emit(OperationCode.CMP_GTE).withComment("condition::greater_equal");
            }
        }
    }

    private void generateExpression(SyntaxTree node) {
        List<SyntaxTree> children = node.getChildren();
        
        for(SyntaxTree child: children){
            byte type = child.getType();

            if(type == TERM) {
                generateTerm(child);
            } else if(type == RIGHT_EXPRESSION){
                generateRightExpression(child);
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

        for(SyntaxTree child: children){
            byte type = child.getType();

           if(type == FACTOR) {
                generateFactor(child);
            }  else if(type == RIGHT_TERM) {
                generateRightTerm(child);
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
        for(SyntaxTree child: node.getChildren()){
            byte type = child.getType();

           if(type == NUMERIC) {
                int value = Integer.parseInt(child.getValue());
                emit(OperationCode.PUSH, value).withComment("factor::push_numeric::" + value);
            } else if(type == IDENTIFIER) {
                String identifierName = child.getValue();
                if(variableOffsets.containsKey(identifierName)) {
                    int offset = variableOffsets.get(identifierName);
                    emit(OperationCode.LOAD, offset).withComment("factor::load_variable::" + identifierName);
                } else {
                    error("Undefined variable: " + identifierName);
                    emit(OperationCode.PUSH, 0).withComment("factor::undefined_variable::" + identifierName);
                }
            } else if(type == FUNCTION_CALL) {
                generateFunctionCall(child);
            } else if(type == EXPRESSION) {
               generateExpression(node);
            }
        }
    }

    private void generateFunctionCall(SyntaxTree node) {
        String functionName = null;
        SyntaxTree expressionList = null;

        for(SyntaxTree child: node.getChildren()){
            if(child.getType() == IDENTIFIER) {
                functionName = child.getValue();
            } else if(child.getType() == EXPRESSION_LIST) {
                expressionList = child;
            }
        }

        if(functionName == null) {
            error("Function call missing function name.");
            return;
        }

        int argumentCount = 0;
        if(expressionList != null) {
            for(SyntaxTree child : expressionList.getChildren()){
                if(child.getType() == EXPRESSION) {
                    generateExpression(child);
                    argumentCount++;
                }
            }
        }

        int callAdress = functionTable.getOrDefault(functionName, 0);
        emit(OperationCode.CALL, callAdress).withComment("call::" + functionName + "::args::" + argumentCount);
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

    private void error(String message) {
        System.err.println("CodeGenerator Error: " + message);
    }
}