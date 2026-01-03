package labreport.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class StaticFileHandler implements HttpHandler {

    private final String resourcePath;

    public StaticFileHandler(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

           ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int nRead;

            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            byte[] bytes = buffer.toByteArray();

            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
