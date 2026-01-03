package labreport.db;

import labreport.logging.AppLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

public class DatabaseManager {

    private static final String DATA_DIR = "data";
    private static final String DB_FILE = "labreport.db";
    private static Connection connection;

    /**
     * Initializes the database.
     * - Creates data/ directory if missing
     * - Opens SQLite connection
     * - Initializes schema
     */
    public static void initialize() {
        try {
            ensureDataDirectory();
            openConnection();
            SchemaInitializer.initialize(connection);

            AppLogger.getLogger().info("Database initialized successfully at data/labreport.db");

        } catch (Exception e) {
            AppLogger.getLogger().log(Level.SEVERE, "FATAL: Failed to initialize database", e);
            System.exit(1); // fail fast
        }
    }

    /**
     * Returns the shared database connection.
     */
    public static Connection getConnection() {
        return connection;
    }

    /**
     * Ensures the data directory exists.
     */
    private static void ensureDataDirectory() throws Exception {
        Path dataPath = Paths.get(DATA_DIR);
        if (!Files.exists(dataPath)) {
            Files.createDirectories(dataPath);
        }
    }

    /**
     * Opens a single SQLite connection.
     */
    private static void openConnection() throws SQLException {
        if (connection != null) {
            return;
        }

        String dbUrl = "jdbc:sqlite:" + DATA_DIR + "/" + DB_FILE;
        connection = DriverManager.getConnection(dbUrl);
    }
}
