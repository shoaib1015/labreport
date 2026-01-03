package labreport.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LogoutHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String sessionId = null;

            if (exchange.getRequestHeaders().containsKey("Cookie")) {
                for (String cookie : exchange.getRequestHeaders().get("Cookie")) {
                    for (String part : cookie.split(";")) {
                        part = part.trim();
                        if (part.startsWith("SESSION_ID=")) {
                            sessionId = part.substring("SESSION_ID=".length());
                        }
                    }
                }
            }

            if (sessionId != null) {
                SessionManager.invalidate(sessionId);
            }

            // Expire cookie in browser
            exchange.getResponseHeaders().add(
                    "Set-Cookie",
                    "SESSION_ID=deleted; Max-Age=0");

            exchange.getResponseHeaders().add("Location", "/login.html");
            exchange.sendResponseHeaders(302, -1);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
