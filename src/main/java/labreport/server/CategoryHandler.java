package labreport.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import labreport.auth.FormParser;
import labreport.auth.CategoryService;
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

public class CategoryHandler implements HttpHandler {

    private static final Logger log = AppLogger.getLogger();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method) && path.endsWith("/api/categories")) {
                handleGetAllCategories(exchange);
            } else if ("GET".equals(method) && path.matches(".*/api/categories/\\d+")) {
                int categoryId = extractId(path);
                handleGetCategoryById(exchange, categoryId);
            } else if ("POST".equals(method) && path.endsWith("/api/categories")) {
                handleAddCategory(exchange);
            } else if ("POST".equals(method) && path.matches(".*/api/categories/\\d+")) {
                int categoryId = extractId(path);
                handleUpdateCategory(exchange, categoryId);
            } else if ("DELETE".equals(method) && path.matches(".*/api/categories/\\d+")) {
                int categoryId = extractId(path);
                handleDeleteCategory(exchange, categoryId);
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
            }

        } catch (Exception e) {
            log.severe("Category handler error: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleGetAllCategories(HttpExchange exchange) throws IOException {
        try {
            List<Map<String, String>> categories = CategoryService.getAllCategories();

            String response = listToJson(categories);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }

            log.info("Categories fetched successfully, count: " + categories.size());

        } catch (Exception e) {
            log.severe("Failed to get categories: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleGetCategoryById(HttpExchange exchange, int categoryId) throws IOException {
        try {
            Map<String, String> category = CategoryService.getCategoryById(categoryId);

            String response = toJson(category);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }

            log.info("Category fetched successfully: id=" + categoryId);

        } catch (Exception e) {
            log.severe("Failed to get category by id: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleAddCategory(HttpExchange exchange) throws IOException {
        try {
            String body = readBody(exchange.getRequestBody());
            Map<String, String> params = FormParser.parse(body);

            String categoryName = params.get("category_name");
            String description = params.get("description");
            String status = params.get("status");

            if (categoryName == null || categoryName.trim().isEmpty()) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, -1);
                return;
            }

            boolean success = CategoryService.addCategory(categoryName, description, status);

            if (success) {
                String response = "{\"status\": \"success\", \"message\": \"Category added successfully\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }

                log.info("Category added successfully");
            } else {
                // CategoryService returns false when the category already exists
                String response = "{\"status\": \"error\", \"message\": \"Category already exists\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_CONFLICT, response.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
                log.info("Category add failed due to duplicate: " + categoryName);
            }

        } catch (Exception e) {
            log.severe("Failed to add category: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleUpdateCategory(HttpExchange exchange, int categoryId) throws IOException {
        try {
            String body = readBody(exchange.getRequestBody());
            Map<String, String> params = FormParser.parse(body);

            String categoryName = params.get("category_name");
            String description = params.get("description");
            String status = params.get("status");

            if (categoryName == null || categoryName.trim().isEmpty()) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, -1);
                return;
            }

            boolean success = CategoryService.updateCategory(categoryId, categoryName, description, status);

            if (success) {
                String response = "{\"status\": \"success\", \"message\": \"Category updated successfully\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }

                log.info("Category updated successfully: id=" + categoryId);
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            }

        } catch (Exception e) {
            log.severe("Failed to update category: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleDeleteCategory(HttpExchange exchange, int categoryId) throws IOException {
        try {
            boolean success = CategoryService.deleteCategory(categoryId);

            if (success) {
                String response = "{\"status\": \"success\", \"message\": \"Category deleted successfully\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }

                log.info("Category deleted successfully: id=" + categoryId);
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            }

        } catch (Exception e) {
            log.severe("Failed to delete category: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private String toJson(Map<String, String> map) {
        StringBuilder json = new StringBuilder("{");
        map.forEach((key, value) -> {
            json.append("\"").append(key).append("\":\"").append(escapeJson(value)).append("\",");
        });
        if (json.length() > 1) {
            json.deleteCharAt(json.length() - 1);
        }
        json.append("}");
        return json.toString();
    }

    private String listToJson(List<Map<String, String>> list) {
        StringBuilder json = new StringBuilder("[");
        for (Map<String, String> map : list) {
            json.append(toJson(map)).append(",");
        }
        if (json.length() > 1) {
            json.deleteCharAt(json.length() - 1);
        }
        json.append("]");
        return json.toString();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private int extractId(String path) {
        String[] parts = path.split("/");
        return Integer.parseInt(parts[parts.length - 1]);
    }

    private String readBody(InputStream is) throws IOException {
        try (Scanner scanner = new Scanner(is).useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
}
