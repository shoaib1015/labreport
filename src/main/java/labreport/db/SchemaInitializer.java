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
                { "users", "CREATE TABLE IF NOT EXISTS users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "username TEXT UNIQUE NOT NULL," +
                        "password_hash TEXT NOT NULL," +
                        "role TEXT NOT NULL," +
                        "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                        ")" },
                { "patients", "CREATE TABLE IF NOT EXISTS patients (" +
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
                        ")" },
                { "test_order", "CREATE TABLE IF NOT EXISTS test_order (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "patient_id INTEGER NOT NULL," +
                        "priority TEXT DEFAULT 'Routine'," +
                        "notes TEXT," +
                        "sample_collected_at TEXT," +
                        "status TEXT DEFAULT 'Pending'," +
                        "created_by INTEGER NOT NULL," +
                        "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                        "updated_at TEXT," +
                        "panel_id INTEGER NOT NULL," +
                        "panel_name TEXT NOT NULL," +
                        "FOREIGN KEY(patient_id) REFERENCES patients(id)," +
                        "FOREIGN KEY(panel_id) REFERENCES panels(panel_id)" +
                        ")" },
                { "components", "CREATE TABLE IF NOT EXISTS components (" +
                        "component_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "panel_name TEXT NOT NULL," +
                        "component_name TEXT NOT NULL," +
                        "unit TEXT," +
                        "normal_range TEXT," +
                        "remarks TEXT," +
                        "ageRange TEXT," +
                        "gender TEXT," +
                        "status TEXT DEFAULT 'Active'," +
                        "created_at TEXT DEFAULT (datetime('now'))," +
                        "updated_at TEXT," +
                        "panel_id INTEGER NOT NULL DEFAULT 0," +
                        "FOREIGN KEY(panel_id) REFERENCES panels(panel_id)" +
                        ")" },
                { "reports", "CREATE TABLE IF NOT EXISTS reports (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "patient_id INTEGER," +
                        "report_date TEXT DEFAULT CURRENT_TIMESTAMP" +
                        ")" },
                { "report_results", "CREATE TABLE IF NOT EXISTS report_results (" +
                        "report_id INTEGER," +
                        "test_id INTEGER," +
                        "value TEXT" +
                        ")" },
                { "lab_profile", "CREATE TABLE IF NOT EXISTS lab_profile (" +
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
                        "updated_at TEXT DEFAULT (datetime('now'))" +
                        ")" },
                { "categories", "CREATE TABLE IF NOT EXISTS categories (" +
                        "category_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "category_name TEXT NOT NULL UNIQUE," +
                        "description TEXT," +
                        "status TEXT DEFAULT 'Active'," +
                        "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                        ")" },
                { "referring_doctors", "CREATE TABLE IF NOT EXISTS referring_doctors (" +
                        "doctor_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "full_name	TEXT NOT NULL," +
                        "license_number	TEXT NOT NULL UNIQUE," +
                        "specialization	TEXT," +
                        "clinic_name	TEXT," +
                        "clinic_address	TEXT," +
                        "contact_number	TEXT," +
                        "email	TEXT UNIQUE," +
                        "referral_code	TEXT UNIQUE," +
                        "preferred_contact_method TEXT," +
                        "status	TEXT DEFAULT 'Active'," +
                        "experience_years NUMERIC," +
                        "consultation_hours	TEXT," +
                        "remarks TEXT," +
                        "created_at	TEXT" +
                        ")" },
                { "panels", "CREATE TABLE IF NOT EXISTS panels (" +
                        "panel_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "panel_name TEXT NOT NULL," +
                        "category_id INTEGER NOT NULL," +
                        "description TEXT," +
                        "price DECIMAL(10,2)," +
                        "status TEXT DEFAULT 'Active'," +
                        "category_name TEXT," +
                        "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                        "updated_at TEXT" +
                        ")" },
                { "test_order_component", "CREATE TABLE IF NOT EXISTS test_order_component (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "test_order_id INTEGER NOT NULL," +
                        "component_id INTEGER NOT NULL," +
                        "component_name TEXT NOT NULL," +
                        "unit TEXT," +
                        "reference_range TEXT," +
                        "result_value TEXT," +
                        "flag TEXT DEFAULT 'Normal'," +
                        "created_at TEXT DEFAULT (datetime('now'))," +
                        "updated_at TEXT," +
                        "FOREIGN KEY(test_order_id) REFERENCES test_order(id)," +
                        "FOREIGN KEY(component_id) REFERENCES components(component_id)" +
                        ")" }
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
