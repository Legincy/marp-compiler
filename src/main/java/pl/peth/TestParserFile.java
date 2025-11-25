package pl.peth;

import java.io.File;
import java.time.Duration;
import java.time.Instant;

import pl.peth.common.parser.Parser;
import pl.peth.common.parser.SyntaxTree;
import pl.peth.common.scanner.Scanner;

class TestParserFiles {
    public static void main(String[] args) {
        Instant start = Instant.now();

        String[] testFiles = {
            "examples/scanner/add.mp",
            "examples/scanner/multiply.mp",
            "examples/scanner/divide.mp",
            "examples/scanner/complex.mp",
            "examples/scanner/condition.mp"
        };

        for (String filename : testFiles) {
            Instant startFile = Instant.now();
            testFile(filename);
            
            Duration durationFile = Duration.between(startFile, Instant.now());
            System.out.println("Time taken for [" + filename + "]: " + durationFile.toMillis() + " ms");
        }

        Duration totalDuration = Duration.between(start, Instant.now());
        System.out.println("Total time taken for all files: " + totalDuration.toMillis() + " ms");
    }

    private static void testFile(String filename) {
        System.out.println("[Test]: Parsing File - " + filename);
        System.out.println("----------------------------------------");

        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("File not found: " + filename);
            System.out.println("Current directory: " + new File(".").getAbsolutePath());
            System.out.println();
            return;
        }

        Scanner scanner = new Scanner();
        if (!scanner.scanFile(filename)) {
            System.out.println("Scanner failed for file: " + filename);
            System.out.println();
            return;
        }

        System.out.println("Tokens found: " + scanner.getTokens().size());

        Parser parser = new Parser();
        SyntaxTree tree = parser.parse(scanner);

        if (tree == null) {
            System.out.println("Parser failed for file: " + filename);
            System.out.println();
            return;
        }

        System.out.println("\nSyntax Tree:");
        tree.print();
        System.out.println("Successfully completed parsing: " + filename);
        System.out.println();
    }
}