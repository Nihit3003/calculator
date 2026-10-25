package com.nihit.calculator;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CalculatorHttpServerTest {
    private static CalculatorHttpServer server;
    private static String base;
    private static final HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    static void start() throws Exception {
        server = new CalculatorHttpServer(0); // 0 = any free port
        server.start();
        base = "http://localhost:" + server.getPort();
    }

    @AfterAll
    static void stop() { server.stop(); }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test void healthIsUp() throws Exception {
        HttpResponse<String> r = get("/health");
        assertEquals(200, r.statusCode());
        assertEquals("{\"status\":\"UP\"}", r.body());
    }

    @Test void addReturnsJson() throws Exception {
        HttpResponse<String> r = get("/api/v1/add?a=2&b=3");
        assertEquals(200, r.statusCode());
        assertEquals("{\"operation\":\"add\",\"a\":2,\"b\":3,\"result\":5}", r.body());
        assertTrue(r.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
    }

    @Test void subtractReturnsJson() throws Exception {
        assertEquals("{\"operation\":\"subtract\",\"a\":10,\"b\":4,\"result\":6}", get("/api/v1/subtract?a=10&b=4").body());
    }

    @Test void multiplyReturnsJson() throws Exception {
        assertEquals("{\"operation\":\"multiply\",\"a\":6,\"b\":7,\"result\":42}", get("/api/v1/multiply?a=6&b=7").body());
    }

    @Test void divideReturnsJson() throws Exception {
        assertEquals("{\"operation\":\"divide\",\"a\":10,\"b\":4,\"result\":2.5}", get("/api/v1/divide?a=10&b=4").body());
    }

    @Test void divideByZeroIs400() throws Exception {
        HttpResponse<String> r = get("/api/v1/divide?a=1&b=0");
        assertEquals(400, r.statusCode());
        assertEquals("{\"error\":\"Division by zero is not allowed\"}", r.body());
    }

    @Test void missingParameterIs400() throws Exception {
        HttpResponse<String> r = get("/api/v1/add?a=1");
        assertEquals(400, r.statusCode());
        assertEquals("{\"error\":\"Missing required number: b\"}", r.body());
    }

    @Test void invalidNumberIs400() throws Exception {
        HttpResponse<String> r = get("/api/v1/add?a=abc&b=1");
        assertEquals(400, r.statusCode());
        assertTrue(r.body().contains("Invalid number for a"));
    }

    @Test void unknownOperationIs400() throws Exception {
        assertEquals(400, get("/api/v1/power?a=1&b=2").statusCode());
    }

    @Test void unknownPathIs404() throws Exception {
        assertEquals(404, get("/nope").statusCode());
    }

    @Test void postIsRejectedWith405() throws Exception {
        HttpResponse<String> r = client.send(HttpRequest.newBuilder(URI.create(base + "/api/v1/add?a=1&b=2"))
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(405, r.statusCode());
    }
}
