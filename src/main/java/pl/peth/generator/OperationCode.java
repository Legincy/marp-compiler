package pl.peth.generator;

public enum OperationCode {
    // == Stack Operations ==
    PUSH,                    // Move a value onto the stack
    LOAD,                    // Load a value from a variable onto the stack
    STORE,                   // Store a value from the stack into a variable
    POP,                     // Remove the top value from the stack

    // == Arithmetic Operations ==
    ADD,                     // Addition: pop b, pop a, push (a + b)
    SUB,                     // Subtraction: pop b, pop a, push (a - b)
    MUL,                     // Multiplication: pop b, pop a, push (a * b)
    DIV,                     // Division: pop b, pop a, push (a / b)
    NEG,                     // Negation: pop a, push (-a)
    
    // == Comparison Operations ==
    CMP_EQ,                  // Equality comparison: pop b, pop a, push (a == b)
    CMP_NEQ,                 // Inequality comparison: pop b, pop a, push (a != b)
    CMP_GT,                  // Greater than comparison: pop b, pop a, push (a > b)
    CMP_LT,                  // Less than comparison: pop b, pop a, push (a < b)
    CMP_GTE,                 // Greater than or equal comparison: pop b, pop a, push (a >= b)
    CMP_LTE,                 // Less than or equal comparison: pop b, pop a, push (a <= b)

    // == Jump Operations ==
    JMP,                     // Unconditional jump to address
    JZ,                      // Jump to address if top of stack is zero (false)
    JNZ,                     // Jump to address if top of stack is non-zero (true)

    // == Function Operations ==
    CALL,                    // Call function at address
    RET,                     // Return from function
    ENTER,                   // Build stack frame for function
    LEAVE,                   // Remove stack frame for function

    // == Other Operations ==
    NOP,                     // No operation
    HALT,                    // Stop execution
    PRINT                    // Print top value of the stack


    
}
