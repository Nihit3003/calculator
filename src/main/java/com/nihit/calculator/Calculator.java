package com.nihit.calculator;

/**
 * Pure arithmetic. Uses {@code double} (64-bit floating point).
 * Limitation: binary floating point, so 0.1 + 0.2 = 0.30000000000000004.
 */
public final class Calculator {

    public double add(double a, double b) {
        return a + b;
    }

    public double subtract(double a, double b) {
        return a - b;
    }

    public double multiply(double a, double b) {
        return a * b;
    }

    /** @throws ArithmeticException if {@code b} is zero */
    public double divide(double a, double b) {
        if (b == 0) {
            throw new ArithmeticException("Division by zero is not allowed");
        }
        return a / b;
    }
}
