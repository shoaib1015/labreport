package labreport;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpContext;


import labreport.auth.AuthHandler;
import labreport.auth.LogoutHandler;
import labreport.auth.UserService;
import labreport.config.AppConfig;
import labreport.db.DatabaseManager;
import labreport.logging.AppLogger;
import labreport.server.AuthFilter;
import labreport.server.CorsFilter;
import labreport.server.SecureTestHandler;
import labreport.server.ShutdownHandler;
import labreport.server.StaticFileHandler;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Main {

    public static void main(String[] args) {
        try {
            
            // Load application configuration
            AppConfig.load();
            
            // Initialize logging
            AppLogger.init();
            Logger log = AppLogger.getLogger();
            log.info("Logger Initialization Complete");
            
            log.fine("*** DB STARTING ***");
            // 1. Initialize database (creates DB if missing)
            DatabaseManager.initialize();

            log.info("*** Ensuring Default Admin USER ***");
            // 2. Ensure default admin user exists
            UserService.ensureDefaultAdmin();

            // 3. Start HTTP server
            int port = 8080;
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new HealthHandler());
            server.setExecutor(null);
            server.createContext("/login", new AuthHandler())
                    .getFilters().add(new CorsFilter());
            server.createContext("/", new HealthHandler());

            HttpContext appContext =
             server.createContext("/app.html",
                new StaticFileHandler("/web/app.html"));
            appContext.getFilters().add(new CorsFilter());
            
            HttpContext secureContext =
                    server.createContext("/secure-test", new SecureTestHandler());

            secureContext.getFilters().add(new CorsFilter());
            secureContext.getFilters().add(new AuthFilter());

            HttpContext styles =
            server.createContext("/styles.css",
                    new StaticFileHandler("/web/styles.css"));
            styles.getFilters().add(new CorsFilter());


           
            // Login page
            HttpContext loginPage =
                    server.createContext("/login.html",
                            new StaticFileHandler("/web/login.html"));
            loginPage.getFilters().add(new CorsFilter());

             HttpContext logoutContext =
            server.createContext("/logout", new LogoutHandler());
            logoutContext.getFilters().add(new CorsFilter());
            logoutContext.getFilters().add(new AuthFilter());



            // server.createContext("/patients", new PatientsHandler())
            //     .getFilters().add(new AuthFilter());
            // server.createContext("/reports", new ReportsHandler())
            //     .getFilters().add(new AuthFilter());

            server.start();

            log.info("*** SERVER STARTED AT http://localhost:" + port + " ***");

            // 4. Graceful shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("*** SHUTDOWN SIGNAL RECEIVED ***");
                server.stop(0);
                log.info("*** SERVER STOPPED ***");
            }));

            server.createContext("/shutdown",
            new ShutdownHandler(() -> {
                System.out.println("Shutting down application...");
                server.stop(0);
                System.exit(0);
            })
            ).getFilters().add(new CorsFilter());


            // 5. Block forever (until JVM is killed)
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
