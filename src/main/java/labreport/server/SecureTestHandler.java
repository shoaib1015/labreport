package labreport.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;

public class SecureTestHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String response = "You are authenticated!<br><br> <a href='/logout'>Logout</a>";

            // exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
