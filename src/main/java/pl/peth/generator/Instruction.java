package pl.peth.generator;

public class Instruction {
    private final OperationCode opCode;
    private final int operand;
    private String label;
    private String comment;

    public Instruction(OperationCode opCode, int operand) {
        this.opCode = opCode;
        this.operand = operand;
        this.label = null;
        this.comment = null;
    }
    
    public Instruction(OperationCode opCode) {
        this(opCode, 0);
    }

    public OperationCode getOpCode() {
        return opCode;
    }

    public int getOperand() {
        return operand;
    }

    public String getLabel() {
        return label;
    }

    public String getComment() {
        return comment;
    }

    public Instruction withLabel(String label) {
        this.label = label;
        return this;
    }

    public Instruction withComment(String comment) {
        this.comment = comment;
        return this;
    }

    public boolean hasOperand() {
        return switch(opCode) {
            case PUSH, LOAD, STORE, GLOAD, GSTORE, JMP, JZ, JNZ, CALL, ENTER, RET -> true;
            default -> false;
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if(label != null) {
            sb.append(label).append(": ");
        }

        sb.append(opCode);

        if(hasOperand()) {
            sb.append(" ").append(operand);
        }

        if(comment != null) {
            sb.append(" ; ").append(comment);
        }

        return sb.toString();
    }

    public String toByteString(int address) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%04d: ", address));
        sb.append(String.format("%-8s", opCode.name()));

        if(hasOperand()) {
            sb.append(String.format("%d", operand));
        } 

        return sb.toString();
    }
}