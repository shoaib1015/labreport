package labreport.auth;

import labreport.db.DatabaseManager;
import labreport.logging.AppLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class TestCatalogService {

    private static final Logger log = AppLogger.getLogger();

    public static List<Map<String, String>> getAllTests() {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, test_name, unit, normal_range, category, price FROM tests ORDER BY test_name");

            ResultSet rs = stmt.executeQuery();

            List<Map<String, String>> tests = new ArrayList<>();
            while (rs.next()) {
                Map<String, String> test = new HashMap<>();
                test.put("id", String.valueOf(rs.getInt("id")));
                test.put("test_name", rs.getString("test_name"));
                test.put("unit", rs.getString("unit"));
                test.put("normal_range", rs.getString("normal_range"));
                test.put("category", rs.getString("category"));
                test.put("price", rs.getString("price"));
                tests.add(test);
            }

            log.info("Fetched " + tests.size() + " tests from database");
            return tests;

        } catch (Exception e) {
            log.severe("Failed to fetch tests: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> getTestById(int testId) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, test_name, unit, normal_range, category, price FROM tests WHERE id = ?");

            stmt.setInt(1, testId);
            ResultSet rs = stmt.executeQuery();

            Map<String, String> test = new HashMap<>();
            if (rs.next()) {
                test.put("id", String.valueOf(rs.getInt("id")));
                test.put("test_name", rs.getString("test_name"));
                test.put("unit", rs.getString("unit"));
                test.put("normal_range", rs.getString("normal_range"));
                test.put("category", rs.getString("category"));
                test.put("price", rs.getString("price"));
            }

            return test;

        } catch (Exception e) {
            log.severe("Failed to fetch test by id: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static boolean addTest(String testName, String unit, String normalRange, String category, String price) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO tests (test_name, unit, normal_range, category, price, created_at) VALUES (?, ?, ?, ?, ?, ?)");

            stmt.setString(1, testName);
            stmt.setString(2, unit);
            stmt.setString(3, normalRange);
            stmt.setString(4, category);
            stmt.setString(5, price);
            stmt.setString(6, LocalDateTime.now().toString());

            int rowsAffected = stmt.executeUpdate();
            log.info("Test added successfully: " + testName + ", rows affected: " + rowsAffected);
            return true;

        } catch (Exception e) {
            log.severe("Failed to add test: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static boolean updateTest(int testId, String testName, String unit, String normalRange, String category, String price) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE tests SET test_name = ?, unit = ?, normal_range = ?, category = ?, price = ? WHERE id = ?");

            stmt.setString(1, testName);
            stmt.setString(2, unit);
            stmt.setString(3, normalRange);
            stmt.setString(4, category);
            stmt.setString(5, price);
            stmt.setInt(6, testId);

            int rowsAffected = stmt.executeUpdate();
            log.info("Test updated successfully: id=" + testId + ", rows affected: " + rowsAffected);
            return true;

        } catch (Exception e) {
            log.severe("Failed to update test: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static boolean deleteTest(int testId) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement("DELETE FROM tests WHERE id = ?");
            stmt.setInt(1, testId);

            int rowsAffected = stmt.executeUpdate();
            log.info("Test deleted successfully: id=" + testId + ", rows affected: " + rowsAffected);
            return true;

        } catch (Exception e) {
            log.severe("Failed to delete test: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
