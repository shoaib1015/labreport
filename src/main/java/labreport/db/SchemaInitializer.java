package labreport.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaInitializer {

    public static void initialize(Connection connection) throws Exception {
        createBasicTables(connection);
        upgradeLabProfileTable(connection);
        upgradeDoctorTable(connection);
        upgradeTestCategoryTable(connection);
        upgradeTestTable(connection);
        upgradeSubTestTable(connection);
    }

    private static void createBasicTables(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password_hash TEXT NOT NULL," +
                "role TEXT NOT NULL," +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS patients (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "age INTEGER," +
                "gender TEXT," +
                "phone TEXT" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS reports (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "patient_id INTEGER," +
                "report_date TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS report_results (" +
                "report_id INTEGER," +
                "test_id INTEGER," +
                "value TEXT" +
                ")"
            );
        }
    }

    private static void upgradeLabProfileTable(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS lab_profile (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "lab_name TEXT," +
                "address TEXT," +
                "city TEXT," +
                "state TEXT," +
                "postal_code TEXT," +
                "country TEXT," +
                "phones TEXT," +
                "email TEXT," +
                "website TEXT," +
                "operating_hours TEXT," +
                "owner_name TEXT," +
                "owner_phone TEXT," +
                "owner_email TEXT," +
                "franchise_type TEXT," +
                "report_header TEXT," +
                "report_footer TEXT," +
                "report_disclaimer TEXT," +
                "logo_data BLOB," +
                "gst_number TEXT," +
                "gst_rate REAL," +
                "enable_gst INTEGER DEFAULT 0," +
                "currency TEXT," +
                "created_at INTEGER," +
                "updated_at INTEGER" +
                ")"
            );
        }
    }

    private static void upgradeDoctorTable(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS doctors (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "doctor_name TEXT," +
                "specialization TEXT," +
                "qualification TEXT," +
                "experience INTEGER," +
                "registration_number TEXT," +
                "primary_phone TEXT," +
                "secondary_phone TEXT," +
                "email TEXT," +
                "fax TEXT," +
                "clinic_name TEXT," +
                "clinic_address TEXT," +
                "clinic_city TEXT," +
                "clinic_state TEXT," +
                "clinic_phone TEXT," +
                "clinic_email TEXT," +
                "default_commission_rate REAL DEFAULT 0," +
                "settlement_type TEXT," +
                "notes TEXT," +
                "status TEXT DEFAULT 'Active'," +
                "created_at INTEGER," +
                "updated_at INTEGER" +
                ")"
            );
        }
    }

    private static void upgradeTestCategoryTable(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS test_categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "category_name TEXT," +
                "category_description TEXT," +
                "display_order INTEGER DEFAULT 0," +
                "status TEXT DEFAULT 'Active'," +
                "created_at INTEGER," +
                "updated_at INTEGER" +
                ")"
            );
        }
    }

    private static void upgradeTestTable(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS tests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "test_name TEXT NOT NULL," +
                "category_id INTEGER," +
                "base_price REAL," +
                "unit TEXT," +
                "description TEXT," +
                "bold_format INTEGER DEFAULT 0," +
                "border_format INTEGER DEFAULT 0," +
                "highlight_format INTEGER DEFAULT 0," +
                "commission_percentage REAL," +
                "status TEXT DEFAULT 'Active'," +
                "created_at INTEGER," +
                "updated_at INTEGER" +
                ")"
            );
        }
    }

    private static void upgradeSubTestTable(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS sub_tests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "test_id INTEGER," +
                "sub_test_name TEXT," +
                "unit TEXT," +
                "normal_range_min REAL," +
                "normal_range_max REAL," +
                "age_ranges TEXT," +
                "price REAL," +
                "instructions TEXT," +
                "print_instructions INTEGER DEFAULT 0," +
                "display_order INTEGER DEFAULT 0," +
                "status TEXT DEFAULT 'Active'," +
                "created_at INTEGER," +
                "updated_at INTEGER" +
                ")"
            );
        }
    }
}
