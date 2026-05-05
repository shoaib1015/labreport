package labreport.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import labreport.auth.FormParser;
import labreport.auth.TestCatalogService;
import labreport.logging.AppLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

public class TestCatalogHandler implements HttpHandler {

    private static final Logger log = AppLogger.getLogger();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method) && path.endsWith("/api/tests")) {
                handleGetAllTests(exchange);
            } else if ("GET".equals(method) && path.matches(".*/api/tests/\\d+")) {
                int testId = extractId(path);
                handleGetTestById(exchange, testId);
            } else if ("POST".equals(method) && path.endsWith("/api/tests")) {
                handleAddTest(exchange);
            } else if ("POST".equals(method) && path.matches(".*/api/tests/\\d+")) {
                int testId = extractId(path);
                handleUpdateTest(exchange, testId);
            } else if ("DELETE".equals(method) && path.matches(".*/api/tests/\\d+")) {
                int testId = extractId(path);
                handleDeleteTest(exchange, testId);
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
            }

        } catch (Exception e) {
            log.severe("Test catalog handler error: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleGetAllTests(HttpExchange exchange) throws IOException {
        try {
            List<Map<String, String>> tests = TestCatalogService.getAllTests();

            String response = listToJson(tests);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }

            log.info("Tests fetched successfully, count: " + tests.size());

        } catch (Exception e) {
            log.severe("Failed to get tests: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleGetTestById(HttpExchange exchange, int testId) throws IOException {
        try {
            Map<String, String> test = TestCatalogService.getTestById(testId);

            String response = toJson(test);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }

            log.info("Test fetched successfully: id=" + testId);

        } catch (Exception e) {
            log.severe("Failed to get test by id: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleAddTest(HttpExchange exchange) throws IOException {
        try {
            String body = readBody(exchange.getRequestBody());
            Map<String, String> params = FormParser.parse(body);

            String testName = params.get("test_name");
            String unit = params.get("unit");
            String normalRange = params.get("normal_range");
            String category = params.get("category");
            String price = params.get("price");

            if (testName == null || testName.trim().isEmpty()) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, -1);
                return;
            }

            boolean success = TestCatalogService.addTest(testName, unit, normalRange, category, price);

            if (success) {
                String response = "{\"status\": \"success\", \"message\": \"Test added successfully\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }

                log.info("Test added successfully");
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            }

        } catch (Exception e) {
            log.severe("Failed to add test: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleUpdateTest(HttpExchange exchange, int testId) throws IOException {
        try {
            String body = readBody(exchange.getRequestBody());
            Map<String, String> params = FormParser.parse(body);

            String testName = params.get("test_name");
            String unit = params.get("unit");
            String normalRange = params.get("normal_range");
            String category = params.get("category");
            String price = params.get("price");

            if (testName == null || testName.trim().isEmpty()) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, -1);
                return;
            }

            boolean success = TestCatalogService.updateTest(testId, testName, unit, normalRange, category, price);

            if (success) {
                String response = "{\"status\": \"success\", \"message\": \"Test updated successfully\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }

                log.info("Test updated successfully");
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            }

        } catch (Exception e) {
            log.severe("Failed to update test: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleDeleteTest(HttpExchange exchange, int testId) throws IOException {
        try {
            boolean success = TestCatalogService.deleteTest(testId);

            if (success) {
                String response = "{\"status\": \"success\", \"message\": \"Test deleted successfully\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }

                log.info("Test deleted successfully");
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            }

        } catch (Exception e) {
            log.severe("Failed to delete test: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private int extractId(String path) {
        String[] parts = path.split("/");
        return Integer.parseInt(parts[parts.length - 1]);
    }

    private String readBody(InputStream is) {
        Scanner s = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private String listToJson(List<Map<String, String>> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String toJson(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":\"")
                    .append(escapeJson(entry.getValue())).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
