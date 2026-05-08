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

public class CategoryService {

    private static final Logger log = AppLogger.getLogger();

    public static List<Map<String, String>> getAllCategories() {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT category_id, category_name, description, status FROM Categories ORDER BY category_name");

            ResultSet rs = stmt.executeQuery();

            List<Map<String, String>> categories = new ArrayList<>();
            while (rs.next()) {
                Map<String, String> category = new HashMap<>();
                category.put("id", String.valueOf(rs.getInt("category_id")));
                category.put("category_name", rs.getString("category_name"));
                category.put("description", rs.getString("description"));
                category.put("status", rs.getString("status"));
                categories.add(category);
            }

            log.info("Fetched " + categories.size() + " categories from database");
            return categories;

        } catch (Exception e) {
            log.severe("Failed to fetch categories: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> getCategoryById(int categoryId) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT category_id, category_name, description, status FROM Categories WHERE category_id = ?");

            stmt.setInt(1, categoryId);
            ResultSet rs = stmt.executeQuery();

            Map<String, String> category = new HashMap<>();
            if (rs.next()) {
                category.put("id", String.valueOf(rs.getInt("category_id")));
                category.put("category_name", rs.getString("category_name"));
                category.put("description", rs.getString("description"));
                category.put("status", rs.getString("status"));
            }

            return category;

        } catch (Exception e) {
            log.severe("Failed to fetch category by id: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static boolean addCategory(String categoryName, String description, String status) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO Categories (category_name, description, status, created_at) VALUES (?, ?, ?, ?)");

            stmt.setString(1, categoryName);
            stmt.setString(2, description);
            stmt.setString(3, status != null && !status.isEmpty() ? status : "Active");
            stmt.setString(4, LocalDateTime.now().toString());

            int rowsAffected = stmt.executeUpdate();
            log.info("Category added successfully: " + categoryName + ", rows affected: " + rowsAffected);
            return true;

        } catch (Exception e) {
            log.severe("Failed to add category: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static boolean updateCategory(int categoryId, String categoryName, String description, String status) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE Categories SET category_name = ?, description = ?, status = ? WHERE category_id = ?");

            stmt.setString(1, categoryName);
            stmt.setString(2, description);
            stmt.setString(3, status != null && !status.isEmpty() ? status : "Active");
            stmt.setInt(4, categoryId);

            int rowsAffected = stmt.executeUpdate();
            log.info("Category updated successfully: id=" + categoryId + ", rows affected: " + rowsAffected);
            return true;

        } catch (Exception e) {
            log.severe("Failed to update category: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static boolean deleteCategory(int categoryId) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement("DELETE FROM Categories WHERE category_id = ?");
            stmt.setInt(1, categoryId);

            int rowsAffected = stmt.executeUpdate();
            log.info("Category deleted successfully: id=" + categoryId + ", rows affected: " + rowsAffected);
            return true;

        } catch (Exception e) {
            log.severe("Failed to delete category: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
