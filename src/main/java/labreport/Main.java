package labreport;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import labreport.db.DatabaseManager;
import labreport.logging.AppLogger;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    public static void main(String[] args) {
        try {
            AppLogger.init();
            Logger log = AppLogger.getLogger();

            log.fine("*** APPLICATION STARTING ***");
            log.info("Application starting");


            // 1. Initialize database (creates DB if missing)
            DatabaseManager.initialize();

            // 2. Start HTTP server
            int port = 8080;
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new HealthHandler());
            server.setExecutor(null);
            server.start();

            log.info("*** SERVER STARTED AT http://localhost:" + port + " ***");

            // 3. Graceful shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("*** SHUTDOWN SIGNAL RECEIVED ***");
                server.stop(0);
                log.info("*** SERVER STOPPED ***");
            }));

            // 4. Block forever (until JVM is killed)
            Thread.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            AppLogger.getLogger().log(Level.SEVERE, "FATAL: Application failed to start", e);
            System.exit(1);
        }
    }

    /**
     * Simple health endpoint for sanity check
     */
    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                String response = "Lab Report System is running";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } catch (Exception e) {
                AppLogger.getLogger().log(Level.WARNING, "Health handler failed", e);
            }
        }
    }
}
