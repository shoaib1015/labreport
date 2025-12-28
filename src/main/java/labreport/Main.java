package labreport;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.awt.Desktop;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {

        System.out.println(">>> MAIN METHOD STARTED <<<");

        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new HealthHandler());
        server.setExecutor(null);
        server.start();

        System.out.println(">>> SERVER STARTED AT http://localhost:" + port + " <<<");

        // Try to open a browser process we can wait on when requested
        Process browserProcess = null;
        String url = "http://localhost:" + port + "/";
        if (Arrays.asList(args).contains("--open-browser")) {
            List<String> candidates = Arrays.asList(
                    "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
                    "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
                    "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
                    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                    "C:\\Program Files\\Mozilla Firefox\\firefox.exe"
            );

            try {
                String browserExe = null;
                for (String p : candidates) {
                    if (new File(p).exists()) { browserExe = p; break; }
                }

                if (browserExe != null) {
                    try {
                        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
                        ProcessBuilder pb;
                        if (isWindows) {
                            // Use cmd start /WAIT so the Process reference blocks until window closes
                            pb = new ProcessBuilder("cmd", "/c", "start", "\"\"", "/WAIT", browserExe, "--new-window", url);
                        } else {
                            pb = new ProcessBuilder(browserExe, "--new-window", url);
                        }
                        browserProcess = pb.start();
                        System.out.println(">>> Launched browser: " + browserExe);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(new URI(url));
                        System.out.println(">>> Opened default browser (no process handle)");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (browserProcess != null) {
            try {
                browserProcess.waitFor();
            } finally {
                saveSession();
                server.stop(0);
                System.out.println(">>> SERVER STOPPED AFTER BROWSER CLOSED <<<");
            }
        } else {
            // Fallback: keep server running as before
            Thread.currentThread().join();
        }
    }

    static void saveSession() {
        try {
            File f = new File("session.json");
            try (PrintWriter pw = new PrintWriter(new FileOutputStream(f))) {
                pw.println("{ \"savedAt\": " + System.currentTimeMillis() + " }");
                pw.flush();
            }
            System.out.println(">>> SESSION SAVED TO " + new File("session.json").getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                System.out.println(">>> REQUEST RECEIVED <<<");

                String response = "Lab Report System is running smoothly!";
                exchange.sendResponseHeaders(200, response.getBytes().length);

                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
