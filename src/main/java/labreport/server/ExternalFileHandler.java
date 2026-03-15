package labreport.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Serves a file from the local filesystem (external to the JAR).
 */
public class ExternalFileHandler implements HttpHandler {

    private final File file;

    public ExternalFileHandler(File file) {
        this.file = file;
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            if (file == null || !file.exists() || file.isDirectory()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().add("Content-Type", getContentType(file));
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                exchange.sendResponseHeaders(500, -1);
            } catch (Exception ignored) {
            }
        }
    }

    private String getContentType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".html") || name.endsWith(".htm")) {
            return "text/html; charset=UTF-8";
        }
        if (name.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (name.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }
        if (name.endsWith(".json")) {
            return "application/json; charset=UTF-8";
        }
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (name.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }
}
