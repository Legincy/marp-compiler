package pl.peth.vm;

import java.util.List;

import pl.peth.generator.Instruction;
import pl.peth.generator.OperationCode;

public class StackMachine {
    private static final int STACK_SIZE = 1024;
    private static final int GLOBAL_BASE = 0;

    private final int[] stack;
    private List<String> stringTable;
    private int stackPointer;
    private int framePointer;
    private int programCounter;
    private int globalCounter;
    private boolean isRunning;
    private boolean isDebugMode;

    public StackMachine() {
        this.stack = new int[STACK_SIZE];
        this.stackPointer = 0;
        this.framePointer = 0;
        this.programCounter = 0;
        this.globalCounter = 0;
        this.isRunning = false;
        this.isDebugMode = false;
    }
    
    public void setDebugMode(boolean debugMode) {
        this.isDebugMode = debugMode;
    }

    public int execute(List<Instruction> instructions) {
        this.programCounter = 0;
        this.stackPointer = 0;
        this.framePointer = 0;
        this.isRunning = true;

        if(this.isDebugMode) {
            System.out.println("DEBUG::Starting Stack Machine");
            System.out.println("DEBUG::Global variables: " + this.globalCounter);
        }

        while (this.isRunning && this.programCounter < instructions.size()) {
           Instruction instruction = instructions.get(this.programCounter);

           if(this.isDebugMode) {
              printState(instruction);
           }

           executeInstruction(instruction);
        }

        if(this.isDebugMode) {
            System.out.println("DEBUG::Stack Machine halted");
            System.out.printf("SP: %d, FP: %d, PC: %d%n", stackPointer, framePointer, programCounter);
            if(stackPointer > 0) {
                System.out.println("Top of Stack: " + stack[stackPointer - 1]);
            }
        }

        return stackPointer > 0 ? stack[stackPointer - 1] : 0;
    }

    private void executeInstruction(Instruction instruction) {
        OperationCode opCode = instruction.getOpCode();
        int operand = instruction.getOperand();

        switch(opCode) {
             case PUSH -> {
                push(operand);
                programCounter++;
            }
            case LOAD -> {
                int value = stack[framePointer + operand];
                push(value);
                programCounter++;
            }
            case STORE -> {
                int value = pop();
                stack[framePointer + operand] = value;
                programCounter++;
            }
            case GLOAD -> {
                int value = stack[GLOBAL_BASE + operand];
                push(value);
                programCounter++;
            }
            case GSTORE -> {
                int value = pop();
                stack[GLOBAL_BASE + operand] = value;
                programCounter++;
            }
            case ADD -> {
                int b = pop();
                int a = pop();
                push(a + b);
                programCounter++;
            }
            case SUB -> {
                int b = pop();
                int a = pop();
                push(a - b);
                programCounter++;
            }
            case DIV -> {
                int b = pop();
                int a = pop();
                if(b == 0) {
                    error("ERROR: Division by zero");
                    isRunning = false;
                    return;
                }else {
                    push(a / b);
                }
                programCounter++;
            }
            case MUL -> {
                int b = pop();
                int a = pop();
                push(a * b);
                programCounter++;
            }
            case NEG -> {
                int a = pop();
                push(-a);
                programCounter++;
            }
            case CMP_EQ -> {
                int b = pop();
                int a = pop();
                push(a == b ? 1 : 0);
                programCounter++;
            }
            case CMP_NEQ -> {
                int b = pop();
                int a = pop();
                push(a != b ? 1 : 0);
                programCounter++;
            }
            case CMP_LT -> {
                int b = pop();
                int a = pop();
                push(a < b ? 1 : 0);
                programCounter++;
            }
            case CMP_GT -> {
                int b = pop();
                int a = pop();
                push(a > b ? 1 : 0);
                programCounter++;
            }
            case CMP_LTE -> {
                int b = pop();
                int a = pop();
                push(a <= b ? 1 : 0);
                programCounter++;
            }
            case CMP_GTE -> {
                int b = pop();
                int a = pop();
                push(a >= b ? 1 : 0);
                programCounter++;
            }
            case JMP -> {
                programCounter = operand;
            }
            case JZ -> {
                int value = pop();
                if (value == 0) {
                    programCounter = operand;
                } else {
                    programCounter++;
                }
            }
            case JNZ -> {
                int value = pop();
                if (value != 0) {
                    programCounter = operand;
                } else {
                    programCounter++;
                }
            }
            case CALL -> {
                push(programCounter + 1);
                push(framePointer);
                framePointer = stackPointer;
                programCounter = operand;
            }
            case RET -> {
                int returnValue = pop();
                stackPointer = framePointer;
                framePointer = pop();
                programCounter = pop();
                stackPointer -= operand;
                push(returnValue);
            }
            case ENTER -> {
                stackPointer += operand;
                programCounter++;
            }
            case LEAVE -> {
                stackPointer = framePointer;
                programCounter++;
            }
            case POP -> {
                pop();
                programCounter++;
            }
            case NOP -> {
                programCounter++;
            }
            case HALT -> {
                isRunning = false;
            }
            case PRINT -> {
                if (stackPointer > 0) {
                    ;
                    System.out.println("OUTPUT: " + stack[stackPointer - 1]);
                }
                programCounter++;
            }
            case PRINT_STR -> {
                int index = pop();
                if (stringTable != null && index >= 0 && index < stringTable.size()) {
                    System.out.println("OUTPUT: " + stringTable.get(index));
                } else {
                    error("ERROR: Invalid string index: " + index);
                }
                programCounter++;
            }
            default -> {
                error("ERROR: Unknown operation code: " + opCode);
                isRunning = false;
            }
        }
    }

    private void push(int value) {
        if(stackPointer >= STACK_SIZE) {
            error("ERROR: Stack overflow");
            isRunning = false;
        }
        stack[stackPointer++] = value;
    }

    private int pop() {
        if(stackPointer <= 0) {
            error("ERROR: Stack underflow");
            isRunning = false;
            return 0;
        }
        return stack[--stackPointer];
    }

    private int peek() {
        if(stackPointer <= 0) {
            error("ERROR: Stack underflow on peek");
            isRunning = false;
            return 0;
        }
        return stack[stackPointer - 1];
    }

    private void printState(Instruction instruction) {
        System.out.printf("PC=%04d  SP=%3d  FP=%3d  | %-20s | Stack: ", 
            programCounter, stackPointer, framePointer, instruction.toByteString(programCounter).substring(6));
        
        System.out.print("[");
        int start = Math.max(0, stackPointer - 10);
        for (int i = start; i < stackPointer; i++) {
            if (i > start) System.out.print(", ");
            if(i < globalCounter) {
                System.out.print("G:" + i + ":" + stack[i]);
            }else{
                System.out.print(stack[i]);
            }
        }
        System.out.println("]");
    }

    public void printStack(){
        System.out.print("Stack (SP=" + stackPointer + "): [");
        for (int i = 0; i < stackPointer; i++) {
            if (i > 0) System.out.print(", ");
            if (i < globalCounter) {
                System.out.print("G:" + i + ":" + stack[i]);
            } else {
                System.out.print(stack[i]);
            }
        }
        System.out.println("]");
    }

    public void setGlobalVariableCounter(int count) {
        this.globalCounter = count;
    }

    public void setStringTable(List<String> table) {
        this.stringTable = table;
    }

    private void error(String message) {
        System.err.println(message);
    }
}
