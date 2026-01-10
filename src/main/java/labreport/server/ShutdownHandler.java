package labreport.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class ShutdownHandler implements HttpHandler {

    private final Runnable shutdownAction;

    public ShutdownHandler(Runnable shutdownAction) {
        this.shutdownAction = shutdownAction;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        exchange.sendResponseHeaders(200, -1);

        // Stop server AFTER response
        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}
            shutdownAction.run();
        }).start();
    }
}
