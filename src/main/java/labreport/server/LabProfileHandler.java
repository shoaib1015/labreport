package labreport.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import labreport.auth.FormParser;
import labreport.auth.LabProfileService;
import labreport.logging.AppLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

public class LabProfileHandler implements HttpHandler {

    private static final Logger log = AppLogger.getLogger();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase();

            if ("GET".equals(method)) {
                handleGet(exchange);
            } else if ("POST".equals(method)) {
                handlePost(exchange);
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
            }

        } catch (Exception e) {
            log.severe("Lab profile handler error: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        try {
            Map<String, String> profile = LabProfileService.getLabProfile(1);

            String response = toJson(profile);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }

            log.info("Lab profile fetched successfully");

        } catch (Exception e) {
            log.severe("Failed to get lab profile: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        try {
            String body = readBody(exchange.getRequestBody());
            Map<String, String> params = FormParser.parse(body);

            String labName = params.get("lab_name");
            String address = params.get("address");
            String contactNumber = params.get("contact_number");

            if (labName == null || labName.trim().isEmpty()) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, -1);
                return;
            }

            boolean success = LabProfileService.updateLabProfile(1, labName, address, contactNumber);

            if (success) {
                String response = "{\"status\": \"success\", \"message\": \"Lab profile updated\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }

                log.info("Lab profile updated successfully");
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            }

        } catch (Exception e) {
            log.severe("Failed to update lab profile: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private String readBody(InputStream is) {
        Scanner s = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private String toJson(Map<String, String> map) {
    StringBuilder sb = new StringBuilder("{");
    for (Map.Entry<String, String> entry : map.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        // Escape quotes and control characters
        value = value.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r")
                     .replace("\t", "\\t");
        sb.append("\"").append(key).append("\":\"").append(value).append("\",");
    }
        if (sb.length() > 1) sb.setLength(sb.length() - 1); // remove last comma
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
