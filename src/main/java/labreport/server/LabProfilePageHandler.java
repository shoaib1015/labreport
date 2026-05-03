package labreport.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import labreport.auth.LabProfileService;
import labreport.logging.AppLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

public class LabProfilePageHandler implements HttpHandler {

    private static final Logger log = AppLogger.getLogger();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase();

            if ("GET".equals(method)) {
                handleGet(exchange);
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
            }

        } catch (Exception e) {
            log.severe("Lab profile page handler error: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        try {
            // Fetch lab profile data
            Map<String, String> profile = LabProfileService.getLabProfile(1);
            
            log.fine("Profile data: " + profile);

            // Read the HTML template
            String template = readResource("/web/master/lab.html");
            log.fine("Template loaded, size: " + template.length());

            // Replace placeholders
            String html = template
                    .replace("{{lab_name}}", escapeHtml(profile.getOrDefault("lab_name", "")))
                    .replace("{{address}}", escapeHtml(profile.getOrDefault("address", "")))
                    .replace("{{contact_number}}", escapeHtml(profile.getOrDefault("contact_number", "")));
            
            log.fine("After replacement, size: " + html.length());

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, html.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(html.getBytes(StandardCharsets.UTF_8));
            }

            log.info("Lab profile page served successfully");

        } catch (Exception e) {
            log.severe("Failed to serve lab profile page: " + e.getMessage());
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private String readResource(String path) throws IOException {
        // First, try to read from classpath using context class loader
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path.substring(1));
        
        if (is != null) {
            try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                return scanner.useDelimiter("\\A").next();
            }
        }
        
        // If not found in classpath, try reading from file system
        // This handles both development and packaged scenarios
        String[] possiblePaths = {
            "src/main/resources" + path,  // Development: src/main/resources/web/master/lab.html
            "target/classes" + path       // After build: target/classes/web/master/lab.html
        };
        
        for (String filePath : possiblePaths) {
            Path p = Paths.get(filePath);
            if (Files.exists(p)) {
                return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
            }
        }
        
        throw new IOException("Resource not found: " + path);
    }

    private String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String removeFetchScript(String html) {
        // Remove the fetchLabProfile function call and the function itself
        // This is a simple replacement; in a real app, use a proper template engine
        html = html.replace("        fetchLabProfile();", "");
        // Remove the fetchLabProfile function
        int start = html.indexOf("    function fetchLabProfile() {");
        if (start != -1) {
            int end = html.indexOf("    }", start);
            if (end != -1) {
                end = html.indexOf("    }", end + 1); // find the closing brace of the function
                html = html.substring(0, start) + html.substring(end + 5);
            }
        }
        // Also remove the loading logic that depends on fetch
        html = html.replace("    <div id=\"loading\" class=\"loading\">Loading lab profile...</div>", "");
        html = html.replace("            document.getElementById('loading').style.display = 'none';\n            document.getElementById('formContainer').style.display = 'block';", "            document.getElementById('formContainer').style.display = 'block';");
        html = html.replace("        .catch(error => {\n            console.error('Error:', error);\n            document.getElementById('loading').textContent = 'Error loading lab profile. Please refresh the page.';\n        });", "");
        return html;
    }
}