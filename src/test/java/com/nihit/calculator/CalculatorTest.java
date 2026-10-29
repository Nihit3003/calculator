package com.nihit.calculator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CalculatorTest {
    private final Calculator calc = new Calculator();

    @Test void addsPositiveNumbers() { assertEquals(5.0, calc.add(2, 3)); }
    @Test void addsNegativeNumbers() { assertEquals(-5.0, calc.add(-2, -3)); }
    @Test void subtractsNumbers() { assertEquals(-1.0, calc.subtract(2, 3)); }
    @Test void multipliesNumbers() { assertEquals(6.0, calc.multiply(2, 3)); }
    @Test void multipliesByZero() { assertEquals(0.0, calc.multiply(99, 0)); }
    @Test void dividesNumbers() { assertEquals(2.5, calc.divide(5, 2)); }

    @Test
    void divideByZeroThrows() {
        ArithmeticException ex = assertThrows(ArithmeticException.class, () -> calc.divide(1, 0));
        assertEquals("Division by zero is not allowed", ex.getMessage());
    }

    @Test
    void decimalAdditionHasFloatingPointLimitation() {
        // Documented limitation of double: not exactly 0.3
        assertNotEquals(0.3, calc.add(0.1, 0.2));
        assertEquals(0.3, calc.add(0.1, 0.2), 1e-9);
    }
}
