package pl.peth.vm;

import java.util.List;

import pl.peth.generator.Instruction;
import pl.peth.generator.OperationCode;

public class StackMachine {
    private static final int STACK_SIZE = 1024;

    private final int[] stack;
    private int stackPointer;
    private int framePointer;
    private int programCounter;
    private boolean isRunning;
    private boolean isDebugMode;

    private List<Instruction> instructions;

    public StackMachine() {
        this.stack = new int[STACK_SIZE];
        this.stackPointer = 0;
        this.framePointer = 0;
        this.programCounter = 0;
        this.isRunning = false;
        this.isDebugMode = false;
    }
    
    public void setDebugMode(boolean debugMode) {
        this.isDebugMode = debugMode;
    }

    public int execute(List<Instruction> instructions) {
        this.instructions = instructions;
        this.programCounter = 0;
        this.stackPointer = 0;
        this.framePointer = 0;
        this.isRunning = true;

        if(isDebugMode) {
            System.out.println("DEBUG::Starting Stack Machine");
        }

        while (isRunning && programCounter < instructions.size()) {
           Instruction instruction = instructions.get(programCounter);

           if(isDebugMode) {
              printState(instruction);
           }

           executeInstruction(instruction);
        }

        if(isDebugMode) {
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
                    System.err.println("ERROR: Division by zero");
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
            }
            default -> {
                System.err.println("ERROR: Unknown operation code: " + opCode);
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
        }
        return stack[--stackPointer];
    }

    private int peek() {
        if(stackPointer <= 0) {
            error("ERROR: Stack underflow on peek");
            isRunning = false;
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
            System.out.print(stack[i]);
        }
        System.out.println("]");
    }

    public void printStack(){
        System.out.print("Stack (SP=" + stackPointer + "): [");
        for (int i = 0; i < stackPointer; i++) {
            if (i > 0) System.out.print(", ");
            System.out.print(stack[i]);
        }
        System.out.println("]");
    }

    private void error(String message) {
        System.err.println(message);
    }
}
