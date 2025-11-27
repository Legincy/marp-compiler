package pl.peth;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import pl.peth.common.Token;
import pl.peth.common.scanner.Scanner;
import pl.peth.common.tokens.ITokenWrapper;

class TestScannerFiles implements ITokenWrapper{
    public static void main(String[] args) {
        Instant start = Instant.now();

        String[] testFiles = {
                "examples/scanner/add.mp",
                "examples/scanner/multiply.mp",
                "examples/scanner/divide.mp",
                "examples/scanner/complex.mp",
                "examples/scanner/condition.mp",
                "examples/scanner/loop.mp"
        };

        for (String filename : testFiles) {
            Instant startFile = Instant.now();
            testFile(filename);

            Duration durationFile = Duration.between(startFile, Instant.now());
            System.out.println("Time taken for [" + filename + "]: " + durationFile.toMillis() + " ms");
        }

        Duration duration = Duration.between(start, Instant.now());
        System.out.println("Completed all tests in " + duration.toMillis() + " ms");

    }

    private static void testFile(String filename) {
        System.out.println("\n>>> Testing: " + filename);
        System.out.println("----------------------------------------");

        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("Could not find file: " + filename);
            return;
        }

        Scanner scanner = new Scanner();

        if (!scanner.scanFile(filename)) {
            System.out.println("Scanning failed for file: " + filename + "\n");
            return;
        }

        List<Token> tokens = scanner.getTokens();
        System.out.println("Tokens: " + tokens.size());

        int functions = 0;
        int numbers = 0;
        int operators = 0;

        for (Token token : tokens) {
            if (token.getType() == ITokenWrapper.IDENTIFIER) {
                int index = tokens.indexOf(token);
                if (index + 1 < tokens.size() &&
                        tokens.get(index + 1).getType() == ITokenWrapper.OPEN_PARENTHESIS) {
                    functions++;
                }
            }

            if (token.getType() == ITokenWrapper.NUMERIC) numbers++;
            if (token.getType() == ITokenWrapper.PLUS ||
                    token.getType() == ITokenWrapper.MINUS ||
                    token.getType() == ITokenWrapper.MULTIPLY ||
                    token.getType() == ITokenWrapper.DIVIDE) {
                operators++;
            }
        }

        System.out.println("Functions: " + functions);
        System.out.println("Numerics: " + numbers);
        System.out.println("Operations: " + operators);
        System.out.println("----------------------------------------");

        scanner.printTokens();
    }
}