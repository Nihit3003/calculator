package com.nihit.calculator;

/** Thrown for any user-facing problem: bad operation, missing/invalid number, divide by zero, overflow. */
public class CalculationException extends Exception {
    public CalculationException(String message) {
        super(message);
    }
}
