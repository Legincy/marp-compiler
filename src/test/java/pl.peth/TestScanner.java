package pl.peth;

import pl.peth.common.scanner.Scanner;

class TestScanner {
    public static void main (String[] args) {
        Scanner scanner = new Scanner();

        System.out.println("[Test 1]: Simple Addition Function");
        System.out.println("----------------------------------------");
        String test1 = "add(a: int, b: int) -> int { return a + b }";
        System.out.println("Input: " + test1);
        scanner.setInput(test1);
        if (scanner.scan()) {
            scanner.printTokens();
            System.out.println("Successfully completed Test 1");
        } else {
            System.out.println("Test 1 failed");
        }

        System.out.println("[Test 2]: Simple Multiplication Function");
        System.out.println("----------------------------------------");
        String test2 = "multiply(x: int, y: int) -> int { return x * y }";
        System.out.println("Input: " + test2);
        scanner = new Scanner();
        scanner.setInput(test2);
        if (scanner.scan()) {
            scanner.printTokens();
            System.out.println("Successfully completed Test 2");
        } else {
            System.out.println("Test 2 failed");
        }

        System.out.println("[Test 3]: Addition and Multiplication");
        System.out.println("----------------------------------------");
        String test3 = "calc(a: int, b: int, c: int) -> int { return a * b + c }";
        System.out.println("Input: " + test3);
        scanner = new Scanner();
        scanner.setInput(test3);
        if (scanner.scan()) {
            scanner.printTokens();
            System.out.println("Successfully completed Test 3");
        } else {
            System.out.println("Test 3 failed");
        }

        System.out.println("[Test 4]: Function with Comments");
        System.out.println("----------------------------------------");
        String test4 =
                "// Diese Funktion addiert zwei Zahlen\n" +
                        "add(x: int, y: int) -> int {\n" +
                        "    return x + y  // Rückgabe\n" +
                        "}";
        System.out.println("Input:\n" + test4);
        scanner = new Scanner();
        scanner.setInput(test4);
        if (scanner.scan()) {
            scanner.printTokens();
            System.out.println("Successfully completed Test 4");
        } else {
            System.out.println("Test 4 failed");
        }

        System.out.println("[Test 5]: Error Handling - Invalid Character");
        System.out.println("----------------------------------------");
        String test5 = "add(a: int) -> int { return a @ b }";
        System.out.println("Input: " + test5);
        scanner = new Scanner();
        scanner.setInput(test5);
        if (!scanner.scan()) {
            System.out.println("Successfully completed Test 5");
        } else {
            System.out.println("Test 5 failed");
        }

        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║         Scanner Tests beendet          ║");
        System.out.println("╚════════════════════════════════════════╝");
    }

}