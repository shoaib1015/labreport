package labreport.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

import labreport.config.AppConfig;

public class AppLogger {

    private static final Logger LOGGER = Logger.getLogger("LabReportLogger");
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;

        try {
        // Create logs directory using AppConfig
        String baseDir = AppConfig.getDataDir();
        if (baseDir == null || baseDir.trim().isEmpty()) {
            baseDir = "data"; // safety fallback
        }

        Path logDir = Paths.get(baseDir, "logs");

        if (!Files.exists(logDir)) {
            Files.createDirectories(logDir);
        }

            // Per-run log file
            String runId = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            String logFile = "logs/run-" + runId + ".log";

            FileHandler fileHandler = new FileHandler(logFile, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);

            LOGGER.setUseParentHandlers(false); // no console spam
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);

            LOGGER.info("Logger initialized");
            LOGGER.info("Run ID: " + runId);

            initialized = true;

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "FATAL: Could not initialize logger", e);
        }
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}
