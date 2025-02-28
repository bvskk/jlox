package Lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 2) {
            System.out.println("Usage:\njlox [script]\njlox -bench [benchmarksdirname]");
            System.exit(64);
        } else if (args.length == 2) {
					  if (!args[0].equals("-bench")) {
								System.out.println("Usage:\njlox [script]\njlox -bench [benchmarksdirname]");
					      System.exit(64);
						}
            runBenchmarks(args[1]);
        } else if (args.length == 1) {
						runFile(args[0]);
				} else {
            runPrompt();
        }
    }

		private static void runBenchmarks(String dirname) throws IOException {
				File dir = new File(dirname);
				File[] files = dir.listFiles(new FilenameFilter() {
						public boolean accept(File dir, String name) {
						return name.toLowerCase().endsWith(".lox");
						}
				});
				for (File file: files) {
						String filename = file.getName();
						System.out.print("Running ");
						System.out.println(filename);
						runFile(dirname + "\\" + filename);
						System.out.print("\n\n\n\n");
				}
		}

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in the exit code.
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error.
        if (hadError) return;

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        // Stop if there was a resolution error.
        if (hadError) return;

        interpreter.interpret(statements);
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
                "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }

    private static void report(int line, String where,
                               String message) {
        System.err.println(
                "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }
}
