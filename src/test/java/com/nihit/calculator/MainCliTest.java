package com.nihit.calculator;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

class MainCliTest {
    private final ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBuf = new ByteArrayOutputStream();

    private int run(String... args) {
        return Main.run(args, new PrintStream(outBuf), new PrintStream(errBuf));
    }

    private String out() { return outBuf.toString().trim(); }
    private String err() { return errBuf.toString().trim(); }

    @Test void successPrintsResultAndExitsZero() {
        assertEquals(0, run("add", "2", "3"));
        assertEquals("5", out());
        assertEquals("", err());
    }

    @Test void divisionResult() {
        assertEquals(0, run("divide", "10", "4"));
        assertEquals("2.5", out());
    }

    @Test void divideByZeroExitsOneWithMessage() {
        assertEquals(1, run("divide", "1", "0"));
        assertEquals("Error: Division by zero is not allowed", err());
        assertEquals("", out());
    }

    @Test void invalidNumberExitsOne() {
        assertEquals(1, run("add", "x", "1"));
        assertTrue(err().contains("Invalid number for a"));
    }

    @Test void noArgumentsShowsUsage() {
        assertEquals(2, run());
        assertTrue(err().contains("Usage:"));
    }

    @Test void wrongArgumentCountShowsUsage() {
        assertEquals(2, run("add", "1"));
        assertTrue(err().contains("exactly two numbers"));
    }

    @Test void servePortDefaultsAndParsing() throws Exception {
        assertEquals(8080, Main.servePort(new String[]{"serve"}, null));
        assertEquals(9000, Main.servePort(new String[]{"serve"}, "9000"));
        assertEquals(7000, Main.servePort(new String[]{"serve", "7000"}, "9000"));
        assertThrows(CalculationException.class, () -> Main.servePort(new String[]{"serve", "abc"}, null));
        assertThrows(CalculationException.class, () -> Main.servePort(new String[]{"serve", "70000"}, null));
    }
}
