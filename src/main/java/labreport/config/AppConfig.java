package labreport.config;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class AppConfig {

    private static String dataDir;
    private static String reportPath;
    private static long lastModified = 0;
    private static boolean loaded = false;

    public static synchronized void load() {
        try {
            Properties props = new Properties();

            File configFile = new File("config.properties");

            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    props.load(fis);
                }
                dataDir = props.getProperty("data.dir");
                reportPath = props.getProperty("report.path");
                lastModified = configFile.lastModified();
            }

            // Fallback to local ./data if config missing or empty
            if (dataDir == null || dataDir.trim().isEmpty()) {
                dataDir = "data";
            }
            if (reportPath == null || reportPath.trim().isEmpty()) {
                reportPath = dataDir;
            }

            // Ensure base directory exists
            File baseDir = new File(dataDir);
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }

            loaded = true;
        } catch (Exception e) {
            // Absolute last-resort fallback
            dataDir = "data";
            reportPath = "data";
            loaded = true;
        }
    }

    private static synchronized void ensureLoaded() {
        File configFile = new File("config.properties");
        if (!loaded || (configFile.exists() && configFile.lastModified() != lastModified)) {
            load();
        }
    }

    public static String getDataDir() {
        ensureLoaded();
        return dataDir;
    }

    public static String getReportPath() {
        ensureLoaded();
        return reportPath;
    }

    public static File getDataSubDir(String name) {
        ensureLoaded();
        File dir = new File(dataDir, name);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
}
