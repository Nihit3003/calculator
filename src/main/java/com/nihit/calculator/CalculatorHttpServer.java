package com.nihit.calculator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Tiny HTTP wrapper built on the JDK's own com.sun.net.httpserver (no framework, no dependencies).
 * It exists so Docker Compose has a real health check and Nginx has something to proxy.
 *
 *   GET /health                          -> 200 {"status":"UP"}
 *   GET /api/v1/{add|subtract|multiply|divide}?a=..&b=..
 *                                        -> 200 {"operation":..,"a":..,"b":..,"result":..}
 *                                        -> 400 {"error":".."} for invalid input / divide by zero
 */
public final class CalculatorHttpServer {

    private final HttpServer server;
    private final CalculatorService service = new CalculatorService();

    public CalculatorHttpServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.createContext("/health", this::handleHealth);
        server.createContext("/api/v1/", this::handleCalculate);
        server.createContext("/", ex -> send(ex, 404, "{\"error\":\"Not found\"}"));
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    private void handleHealth(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            send(ex, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        send(ex, 200, "{\"status\":\"UP\"}");
    }

    private void handleCalculate(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            send(ex, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        String path = ex.getRequestURI().getPath();
        String op = path.substring("/api/v1/".length());
        Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());
        try {
            double result = service.calculate(op, q.get("a"), q.get("b"));
            double a = CalculatorService.parseNumber("a", q.get("a"));
            double b = CalculatorService.parseNumber("b", q.get("b"));
            send(ex, 200, "{\"operation\":\"" + op.toLowerCase() + "\",\"a\":" + CalculatorService.format(a)
                    + ",\"b\":" + CalculatorService.format(b)
                    + ",\"result\":" + CalculatorService.format(result) + "}");
        } catch (CalculationException e) {
            send(ex, 400, "{\"error\":\"" + jsonEscape(e.getMessage()) + "\"}");
        }
    }

    static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return map;
        }
        for (String pair : raw.split("&")) {
            int i = pair.indexOf('=');
            String k = i < 0 ? pair : pair.substring(0, i);
            String v = i < 0 ? "" : pair.substring(i + 1);
            map.put(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return map;
    }

    static String jsonEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void send(HttpExchange ex, int status, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }
}
