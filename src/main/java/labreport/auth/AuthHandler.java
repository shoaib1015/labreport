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

public class AuthHandler implements HttpHandler {

    private static final Logger log = AppLogger.getLogger();

    @Override
    public void handle(HttpExchange exchange) {
        try {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
                return;
            }

            String body = readBody(exchange.getRequestBody());
            Map<String, String> params = FormParser.parse(body);

            String username = params.get("username");
            String password = params.get("password");

            if (UserService.validateCredentials(username, password)) {
                String sessionId = SessionManager.createSession(username);

                exchange.getResponseHeaders().add(
                        "Set-Cookie",
                        "SESSION_ID=" + sessionId + "; HttpOnly");

                byte[] response = "LOGIN_OK".getBytes();
                exchange.sendResponseHeaders(200, response.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }

                log.info("User logged in: " + username);

            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, -1);
                log.warning("Failed login attempt for user: " + username);
            }

        } catch (Exception e) {
            log.severe("Login error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String readBody(InputStream is) {
        Scanner s = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
