package labreport.config;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class AppConfig {

    private static String dataDir;

    public static void load() {
        try {
            Properties props = new Properties();

            File configFile = new File("config.properties");

            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    props.load(fis);
                }
                dataDir = props.getProperty("data.dir");
            }

            // Fallback to local ./data if config missing or empty
            if (dataDir == null || dataDir.trim().isEmpty()) {
                dataDir = "data";
            }

            // Ensure base directory exists
            File baseDir = new File(dataDir);
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }

        } catch (Exception e) {
            // Absolute last-resort fallback
            dataDir = "data";
        }
    }

    public static String getDataDir() {
        return dataDir;
    }

    public static File getDataSubDir(String name) {
        File dir = new File(dataDir, name);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
}
