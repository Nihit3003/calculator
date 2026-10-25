package com.nihit.calculator;

import java.io.PrintStream;

/**
 * Entry point.
 *   java -jar calculator.jar add 2 3        -> CLI mode (prints result, exits)
 *   java -jar calculator.jar serve [port]   -> HTTP mode (long-running; used by Docker)
 */
public final class Main {

    static final String USAGE = String.join(System.lineSeparator(),
            "Usage:",
            "  java -jar calculator.jar <add|subtract|multiply|divide> <a> <b>",
            "  java -jar calculator.jar serve [port]     (default port 8080, or $PORT)");

    public static void main(String[] args) throws Exception {
        if (args.length >= 1 && "serve".equalsIgnoreCase(args[0])) {
            int port;
            try {
                port = servePort(args, System.getenv("PORT"));
            } catch (CalculationException e) {
                System.err.println("Error: " + e.getMessage());
                System.exit(2);
                return;
            }
            CalculatorHttpServer server = new CalculatorHttpServer(port);
            server.start();
            System.out.println("Calculator HTTP server listening on port " + server.getPort());
            return; // server threads keep the JVM alive
        }
        System.exit(run(args, System.out, System.err));
    }

    static int servePort(String[] args, String envPort) throws CalculationException {
        String text = args.length >= 2 ? args[1] : envPort;
        if (text == null || text.isBlank()) {
            return 8080;
        }
        try {
            int p = Integer.parseInt(text.trim());
            if (p < 0 || p > 65535) {
                throw new NumberFormatException();
            }
            return p;
        } catch (NumberFormatException e) {
            throw new CalculationException("Invalid port: '" + text + "'");
        }
    }

    /** CLI logic, separated from main() so it can be unit-tested. Returns the exit code. */
    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0) {
            err.println(USAGE);
            return 2;
        }
        if (args.length != 3) {
            err.println("Error: expected an operation and exactly two numbers");
            err.println(USAGE);
            return 2;
        }
        try {
            double result = new CalculatorService().calculate(args[0], args[1], args[2]);
            out.println(CalculatorService.format(result));
            return 0;
        } catch (CalculationException e) {
            err.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
