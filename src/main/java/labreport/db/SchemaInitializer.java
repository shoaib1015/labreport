package labreport.db;

import java.sql.Connection;
import java.sql.Statement;

public class SchemaInitializer {

    public static void initialize(Connection connection) throws Exception {
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
                "CREATE TABLE IF NOT EXISTS tests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "test_name TEXT NOT NULL," +
                "unit TEXT," +
                "normal_range TEXT," +
                "category TEXT," +
                "price DECIMAL(10, 2)," +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
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
            
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS LabProfile (" +
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
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS ReferringDoctors (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "full_name TEXT NOT NULL," +
                "contact_number TEXT," +
                "license_number TEXT," +
                "status TEXT DEFAULT 'Active'," +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
        }
    }
}
