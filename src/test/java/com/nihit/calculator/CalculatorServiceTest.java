package com.nihit.calculator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CalculatorServiceTest {
    private final CalculatorService service = new CalculatorService();

    @Test void add() throws Exception { assertEquals(5.0, service.calculate("add", "2", "3")); }
    @Test void subtract() throws Exception { assertEquals(-1.0, service.calculate("subtract", "2", "3")); }
    @Test void multiply() throws Exception { assertEquals(6.0, service.calculate("multiply", "2", "3")); }
    @Test void divide() throws Exception { assertEquals(2.5, service.calculate("divide", "5", "2")); }
    @Test void operationIsCaseInsensitive() throws Exception { assertEquals(5.0, service.calculate("ADD", "2", "3")); }
    @Test void acceptsDecimalsAndWhitespace() throws Exception { assertEquals(4.0, service.calculate("add", " 1.5 ", "2.5")); }

    @Test
    void divideByZeroGivesClearError() {
        CalculationException ex = assertThrows(CalculationException.class, () -> service.calculate("divide", "1", "0"));
        assertEquals("Division by zero is not allowed", ex.getMessage());
    }

    @Test
    void missingOperand() {
        CalculationException ex = assertThrows(CalculationException.class, () -> service.calculate("add", "1", null));
        assertEquals("Missing required number: b", ex.getMessage());
    }

    @Test
    void blankOperand() {
        assertThrows(CalculationException.class, () -> service.calculate("add", " ", "1"));
    }

    @Test
    void nonNumericOperand() {
        CalculationException ex = assertThrows(CalculationException.class, () -> service.calculate("add", "abc", "1"));
        assertEquals("Invalid number for a: 'abc'", ex.getMessage());
    }

    @Test
    void rejectsNaNAndInfinity() {
        assertThrows(CalculationException.class, () -> service.calculate("add", "NaN", "1"));
        assertThrows(CalculationException.class, () -> service.calculate("add", "Infinity", "1"));
    }

    @Test
    void unknownOperation() {
        CalculationException ex = assertThrows(CalculationException.class, () -> service.calculate("power", "1", "2"));
        assertTrue(ex.getMessage().startsWith("Unknown operation"));
    }

    @Test
    void missingOperation() {
        assertThrows(CalculationException.class, () -> service.calculate(null, "1", "2"));
    }

    @Test
    void overflowIsReported() {
        assertThrows(CalculationException.class, () -> service.calculate("multiply", "1e308", "10"));
    }

    @Test
    void formatsWithoutTrailingZerosOrExponent() {
        assertEquals("5", CalculatorService.format(5.0));
        assertEquals("2.5", CalculatorService.format(2.5));
        assertEquals("0", CalculatorService.format(0.0));
        assertEquals("100000000000", CalculatorService.format(1e11));
    }
}
