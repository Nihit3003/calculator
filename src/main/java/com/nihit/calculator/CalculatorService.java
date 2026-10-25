package com.nihit.calculator;

import java.math.BigDecimal;

/**
 * Validates text input and runs an operation. Shared by the CLI and the HTTP server
 * so both behave identically.
 */
public final class CalculatorService {

    public static final String[] OPERATIONS = {"add", "subtract", "multiply", "divide"};

    private final Calculator calculator = new Calculator();

    /** Parses a number from text; rejects null/blank, non-numbers, NaN and Infinity. */
    public static double parseNumber(String name, String text) throws CalculationException {
        if (text == null || text.isBlank()) {
            throw new CalculationException("Missing required number: " + name);
        }
        double value;
        try {
            value = Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw new CalculationException("Invalid number for " + name + ": '" + text + "'");
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new CalculationException("Invalid number for " + name + ": '" + text + "'");
        }
        return value;
    }

    public double calculate(String operation, String aText, String bText) throws CalculationException {
        if (operation == null || operation.isBlank()) {
            throw new CalculationException("Missing operation (add, subtract, multiply, divide)");
        }
        String op = operation.trim().toLowerCase();
        if (!isSupported(op)) {
            throw new CalculationException("Unknown operation: '" + operation
                    + "'. Use add, subtract, multiply or divide");
        }
        double a = parseNumber("a", aText);
        double b = parseNumber("b", bText);
        double result;
        try {
            switch (op) {
                case "add": result = calculator.add(a, b); break;
                case "subtract": result = calculator.subtract(a, b); break;
                case "multiply": result = calculator.multiply(a, b); break;
                default: result = calculator.divide(a, b); break;
            }
        } catch (ArithmeticException e) {
            throw new CalculationException(e.getMessage());
        }
        if (Double.isInfinite(result) || Double.isNaN(result)) {
            throw new CalculationException("Result is too large to represent");
        }
        return result;
    }

    public static boolean isSupported(String op) {
        for (String s : OPERATIONS) {
            if (s.equals(op)) {
                return true;
            }
        }
        return false;
    }

    /** Plain (non-scientific) text, e.g. 5 instead of 5.0 and 2.5 instead of 2.5E0. */
    public static String format(double value) {
        BigDecimal bd = BigDecimal.valueOf(value).stripTrailingZeros();
        if (bd.signum() == 0) {
            return "0";
        }
        return bd.toPlainString();
    }
}
