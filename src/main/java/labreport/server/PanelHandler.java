package labreport.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import labreport.auth.FormParser;
import labreport.auth.PanelService;
import labreport.logging.AppLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

public class PanelHandler implements HttpHandler {

    private static final Logger log = AppLogger.getLogger();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase();
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
               log.info("Received request: " + method + " " + path); 
            if ("GET".equals(method) && path.endsWith("/api/panels")) {
                handleGetAllPanels(exchange);
            } else if ("GET".equals(method) && path.matches(".*/api/panels/name/.*")) {
                log.info("Received request for panel by name, path: " + path);
                String panelName = extractPanelName(path);
                handleGetPanelByName(exchange, panelName);
            } else if ("GET".equals(method) && path.matches(".*/api/components/panel/\\d+")) {
                log.info("Received request for components by panel ID, path: " + path);
                int panelId = extractComponentsPanelId(path);
                handleGetComponentsByPanel(exchange, panelId);
            } else if ("GET".equals(method) && path.matches(".*/api/panels/\\d+")) {
                int panelId = extractId(path);
                handleGetPanelById(exchange, panelId);
            } else if ("POST".equals(method) && path.endsWith("/api/panels")) {
                handleAddPanel(exchange);
            } else if ("POST".equals(method) && path.matches(".*/api/panels/\\d+")) {
                int panelId = extractId(path);
                handleUpdatePanel(exchange, panelId);
            } else if ("DELETE".equals(method) && path.matches(".*/api/panels/\\d+")) {
                int panelId = extractId(path);
                handleDeletePanel(exchange, panelId);
            } else if ("GET".equals(method) && path.matches(".*/api/commissions/\\d+")) {
                int doctorId = extractId(path);
                handleGetCommission(exchange, doctorId);
            }
            else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
            }

        } catch (Exception e) {
            log.severe("Panel handler error: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleGetAllPanels(HttpExchange exchange) throws IOException {
        try {
            List<Map<String, String>> panels = PanelService.getAllPanels();

            String response = listToJson(panels);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }

            log.info("Panels fetched successfully, count: " + panels.size());

        } catch (Exception e) {
            log.severe("Failed to get panels: " + e.getMessage());
            String errorResponse = "{\"error\": \"Failed to fetch panels\", \"message\": \"" + escapeJson(e.getMessage()) + "\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, errorResponse.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorResponse.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private void handleGetPanelById(HttpExchange exchange, int panelId) throws IOException {
        try {
            Map<String, String> panel = PanelService.getPanelById(panelId);

            String response = toJson(panel);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }

            log.info("Panel fetched successfully: id=" + panelId);

        } catch (Exception e) {
            log.severe("Failed to get panel by id: " + e.getMessage());
            String errorResponse = "{\"error\": \"Failed to fetch panel\", \"message\": \"" + escapeJson(e.getMessage()) + "\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, errorResponse.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorResponse.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private void handleAddPanel(HttpExchange exchange) throws IOException {
        try {
            String body = readBody(exchange.getRequestBody());
            Map<String, String> params = FormParser.parse(body);

            String panelName = params.get("panel_name");
            String categoryId = params.get("category_id");
            String description = params.get("description");
            String price = params.get("price");
            String status = params.get("status");
            String categoryName = params.get("category_name");

            if (panelName == null || panelName.trim().isEmpty()) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, -1);
                return;
            }

            boolean success = PanelService.addPanel(panelName, categoryId, description, price, status, categoryName);

            if (success) {
                String response = "{\"status\": \"success\", \"message\": \"Panel added successfully\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }

                log.info("Panel added successfully");
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            }

        } catch (Exception e) {
            log.severe("Failed to add panel: " + e.getMessage());
            String errorResponse = "{\"error\": \"Failed to add panel\", \"message\": \"" + escapeJson(e.getMessage()) + "\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, errorResponse.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorResponse.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private void handleUpdatePanel(HttpExchange exchange, int panelId) throws IOException {
        try {
            String body = readBody(exchange.getRequestBody());
            Map<String, String> params = FormParser.parse(body);

            String panelName = params.get("panel_name");
            String categoryId = params.get("category_id");
            String description = params.get("description");
            String price = params.get("price");
            String status = params.get("status");
            String categoryName = params.get("category_name");

            if (panelName == null || panelName.trim().isEmpty()) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, -1);
                return;
            }

            boolean success = PanelService.updatePanel(panelId, panelName, categoryId, description, price, status, categoryName);

            if (success) {
                String response = "{\"status\": \"success\", \"message\": \"Panel updated successfully\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }

                log.info("Panel updated successfully: id=" + panelId);
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            }

        } catch (Exception e) {
            log.severe("Failed to update panel: " + e.getMessage());
            String errorResponse = "{\"error\": \"Failed to update panel\", \"message\": \"" + escapeJson(e.getMessage()) + "\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, errorResponse.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorResponse.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private void handleDeletePanel(HttpExchange exchange, int panelId) throws IOException {
        try {
            boolean success = PanelService.deletePanel(panelId);

            if (success) {
                String response = "{\"status\": \"success\", \"message\": \"Panel deleted successfully\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }

                log.info("Panel deleted successfully: id=" + panelId);
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            }

        } catch (Exception e) {
            log.severe("Failed to delete panel: " + e.getMessage());
            String errorResponse = "{\"error\": \"Failed to delete panel\", \"message\": \"" + escapeJson(e.getMessage()) + "\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, errorResponse.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorResponse.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private void handleGetPanelByName(HttpExchange exchange, String panelName) throws IOException {
        try {
            Map<String, String> panel = PanelService.getPanelByName(panelName);

            String response = toJson(panel);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }

            log.info("Panel fetched successfully: name=" + panelName);

        } catch (Exception e) {
            log.severe("Failed to get panel by name: " + e.getMessage());
            String errorResponse = "{\"error\": \"Failed to fetch panel\", \"message\": \"" + escapeJson(e.getMessage()) + "\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, errorResponse.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorResponse.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private void handleGetComponentsByPanel(HttpExchange exchange, int panelId) throws IOException {
        try {
            log.info("Fetching components for panel ID: " + panelId);
            List<Map<String, String>> components = PanelService.getComponentsByPanel(panelId);
            log.info("Fetched components for panel ID: " + panelId + ", count: " + components.size());
            String response = listToJson(components);
            log.info("Response JSON: " + response);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }

            log.info("Components fetched for panel ID: " + panelId + ", count: " + components.size());

        } catch (Exception e) {
            log.severe("Failed to get components for panel ID: " + e.getMessage());
            e.printStackTrace();
            String errorResponse = "{\"error\": \"Failed to fetch components\", \"message\": \"" + escapeJson(e.getMessage()) + "\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, errorResponse.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorResponse.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private void handleGetCommission(HttpExchange exchange, int doctorId) throws IOException {
        try {
            log.info("Fetching commission for doctor ID: " + doctorId);
            Double commission_percent = PatientService.getCommissions(doctorId);

            String response = "{\"commission_percent\": " + commission_percent + "}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }

            log.info("Commissions fetched successfully, count: " + commission_percent);

        } catch (Exception e) {
            log.severe("Failed to get commissions: " + e.getMessage());
            String errorResponse = "{\"error\": \"Failed to fetch commissions\", \"message\": \"" + escapeJson(e.getMessage()) + "\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, errorResponse.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorResponse.getBytes(StandardCharsets.UTF_8));
            }
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

    private String extractPanelName(String path) {
        // Extract panel name from /api/panels/name/{panelName}
        String[] parts = path.split("/");
        if (parts.length > 0) {
            try {
                return java.net.URLDecoder.decode(parts[parts.length - 1], "UTF-8");
            } catch (Exception e) {
                return parts[parts.length - 1];
            }
        }
        return "";
    }

    private String extractComponentsPanelName(String path) {
        // Extract panel name from /api/components/panel/{panelName}
        //String[] parts = path.split("/");
        //if (parts.length > 0) {
         //   try {
           //     return java.net.URLDecoder.decode(parts[parts.length - 1], "UTF-8");
           // } catch (Exception e) {
           //     return parts[parts.length - 1];
           // }
        //}
       // return "";
       // Example path: /api/components/panel/CBC
    // Or: /api/components/panel/Liver%20Function%20Test
        String prefix = "/api/components/panel/";
        int index = path.indexOf(prefix);
        if (index != -1) {
            String encodedName = path.substring(index + prefix.length());
            try {
                // Use "UTF-8" string for compatibility
                return URLDecoder.decode(encodedName, "UTF-8");
            } catch (Exception e) {
                log.severe("Failed to decode panel name: " + e.getMessage());
                return encodedName; // fallback to raw value
            }
        }
        return null;
    }

    private int extractComponentsPanelId(String path) {
        // Extract panel ID from /api/components/panel/{panelId}
        // Example path: /api/components/panel/5
        String prefix = "/api/components/panel/";
        int index = path.indexOf(prefix);
        if (index != -1) {
            String idStr = path.substring(index + prefix.length());
            try {
                return Integer.parseInt(idStr);
            } catch (NumberFormatException e) {
                log.severe("Failed to parse panel ID: " + e.getMessage());
                return -1;
            }
        }
        return -1;
    }

    private String readBody(InputStream is) throws IOException {
        try (Scanner scanner = new Scanner(is).useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
}
