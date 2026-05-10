package labreport.db;

import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Logger;
import labreport.logging.AppLogger;

public class SchemaInitializer {

    private static final Logger log = AppLogger.getLogger();

    public static void initialize(Connection connection) throws Exception {
        // Create each table separately to avoid issues with existing databases
        String[][] tables = {
            {"users", "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password_hash TEXT NOT NULL," +
                "role TEXT NOT NULL," +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")"},
            {"patients", "CREATE TABLE IF NOT EXISTS patients (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "dob TEXT," +
                "gender TEXT," +
                "contact_phone TEXT," +
                "contact_email TEXT," +
                "address TEXT," +
                "referring_doctor_id INTEGER," +
                "created_by INTEGER NOT NULL," +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TEXT" +
                ")"},
            {"test_order", "CREATE TABLE IF NOT EXISTS test_order (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "patient_id INTEGER NOT NULL," +
                "priority TEXT DEFAULT 'Routine'," +
                "notes TEXT," +
                "sample_collected_at TEXT," +
                "created_by INTEGER NOT NULL," +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TEXT," +
                "FOREIGN KEY(patient_id) REFERENCES patients(id)" +
                ")"},
            {"test_order_panel", "CREATE TABLE IF NOT EXISTS test_order_panel (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "test_order_id INTEGER NOT NULL," +
                "panel_id INTEGER NOT NULL," +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY(test_order_id) REFERENCES test_order(id)," +
                "FOREIGN KEY(panel_id) REFERENCES Panels(panel_id)" +
                ")"},
            {"reports", "CREATE TABLE IF NOT EXISTS reports (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "patient_id INTEGER," +
                "report_date TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")"},
            {"report_results", "CREATE TABLE IF NOT EXISTS report_results (" +
                "report_id INTEGER," +
                "test_id INTEGER," +
                "value TEXT" +
                ")"},
            {"LabProfile", "CREATE TABLE IF NOT EXISTS LabProfile (" +
                "lab_id INTEGER PRIMARY KEY," +
                "lab_name TEXT NOT NULL," +
                "registration_number TEXT UNIQUE," +
                "address TEXT," +
                "contact_number TEXT," +
                "email TEXT," +
                "website TEXT," +
                "director_name TEXT," +
                "license_number TEXT," +
                "accreditation TEXT," +
                "status TEXT DEFAULT 'Active'" +
                ")"},
            {"Categories", "CREATE TABLE IF NOT EXISTS Categories (" +
                "category_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "category_name TEXT NOT NULL UNIQUE," +
                "description TEXT," +
                "status TEXT DEFAULT 'Active'," +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")"},
            {"Panels", "CREATE TABLE IF NOT EXISTS Panels (" +
                "panel_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "panel_name TEXT NOT NULL," +
                "category_id INTEGER NOT NULL," +
                "description TEXT," +
                "price DECIMAL(10,2)," +
                "status TEXT DEFAULT 'Active'," +
                "category_name TEXT," +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TEXT" +
                ")"}
        };

        for (String[] table : tables) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(table[1]);
                log.info("Table '" + table[0] + "' created or already exists");
            } catch (Exception e) {
                // Log but continue - table might already exist
                log.warning("Could not create table '" + table[0] + "': " + e.getMessage());
            }
        }

    }
}

