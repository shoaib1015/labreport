package labreport.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import labreport.logging.AppLogger;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

public class UserInfoHandler implements HttpHandler {

    private static final Logger log = AppLogger.getLogger();

    @Override
    public void handle(HttpExchange exchange) {
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
                return;
            }

            // Get session ID from cookies
            String sessionId = extractSessionIdFromCookies(exchange.getRequestHeaders().getFirst("Cookie"));

            if (sessionId == null || !SessionManager.isValid(sessionId)) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, -1);
                return;
            }

            // Get session info
            Map<String, String> sessionInfo = SessionManager.getSessionInfo(sessionId);

            if (sessionInfo == null) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, -1);
                return;
            }

            // Build JSON response manually
            String username = sessionInfo.get("username");
            String role = sessionInfo.get("role");
            
            String jsonResponse = "{\"username\":\"" + escapeJson(username) + "\",\"role\":\"" + escapeJson(role) + "\"}";

            byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }

            log.info("User info retrieved for: " + username);

        } catch (Exception e) {
            log.severe("Error in UserInfoHandler: " + e.getMessage());
            try {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            } catch (Exception ex) {
                log.severe("Failed to send error response");
            }
        }
    }

    private String extractSessionIdFromCookies(String cookieHeader) {
        if (cookieHeader == null || cookieHeader.isEmpty()) {
            return null;
        }

        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            String trimmed = cookie.trim();
            if (trimmed.startsWith("SESSION_ID=")) {
                return trimmed.substring("SESSION_ID=".length());
            }
        }
        return null;
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
