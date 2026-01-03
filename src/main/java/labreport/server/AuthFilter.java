package labreport.server;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import labreport.auth.SessionManager;
import labreport.logging.AppLogger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.logging.Logger;

public class AuthFilter extends Filter {

    private static final Logger log = AppLogger.getLogger();

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {

        String path = exchange.getRequestURI().getPath();

        // 1. Allow public endpoints
        if (isPublicPath(path)) {
            chain.doFilter(exchange);
            return;
        }

        // 2. Extract SESSION_ID from cookies
        String sessionId = extractSessionId(exchange);

        // 3. Validate session
        if (sessionId != null && SessionManager.isValid(sessionId)) {
            chain.doFilter(exchange); // allow request
            return;
        }

        // 4. Block unauthorized access
        log.warning("Unauthorized access attempt: " + path);
        // exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, -1);
        exchange.getResponseHeaders().add("Location", "/login.html");
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_MOVED_TEMP, -1);

        
    }

    @Override
    public String description() {
        return "Authentication filter";
    }

    private boolean isPublicPath(String path) {
        return path.equals("/")
                || path.equals("/login");
    }

    private String extractSessionId(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies == null) return null;

        for (String cookieHeader : cookies) {
            String[] cookiesArr = cookieHeader.split(";");
            for (String cookie : cookiesArr) {
                String trimmed = cookie.trim();
                if (trimmed.startsWith("SESSION_ID=")) {
                    return trimmed.substring("SESSION_ID=".length());
                }
            }
        }
        return null;
    }
}
