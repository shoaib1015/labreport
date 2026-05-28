package labreport.auth;

import labreport.db.DatabaseManager;
import labreport.logging.AppLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class PanelService {

    private static final Logger log = AppLogger.getLogger();

    public static List<Map<String, String>> getAllPanels() {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT panel_id, panel_name, category_name, description, price, status " +
                    "FROM panels");

            ResultSet rs = stmt.executeQuery();

            List<Map<String, String>> panels = new ArrayList<>();
            while (rs.next()) {
                Map<String, String> panel = new HashMap<>();
                panel.put("id", String.valueOf(rs.getInt("panel_id")));
                panel.put("panel_name", rs.getString("panel_name"));
                panel.put("category_name", rs.getString("category_name"));
                panel.put("description", rs.getString("description"));
                panel.put("price", rs.getString("price"));
                panel.put("status", rs.getString("status"));
                panels.add(panel);
            }

            log.info("Fetched " + panels.size() + " panels from database");
            return panels;

        } catch (Exception e) {
            log.severe("Failed to fetch panels: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> getPanelById(int panelId) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT p.panel_id, p.panel_name, p.category_id, p.description, p.price, p.status, c.category_name " +
                    "FROM panels p " +
                    "LEFT JOIN categories c ON p.category_id = c.category_id " +
                    "WHERE p.panel_id = ?");

            stmt.setInt(1, panelId);
            ResultSet rs = stmt.executeQuery();

            Map<String, String> panel = new HashMap<>();
            if (rs.next()) {
                panel.put("id", String.valueOf(rs.getInt("panel_id")));
                panel.put("panel_name", rs.getString("panel_name"));
                panel.put("category_id", String.valueOf(rs.getInt("category_id")));
                panel.put("category_name", rs.getString("category_name"));
                panel.put("description", rs.getString("description"));
                panel.put("price", rs.getString("price"));
                panel.put("status", rs.getString("status"));
            }

            return panel;

        } catch (Exception e) {
            log.severe("Failed to fetch panel by id: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static boolean addPanel(String panelName, String categoryId, String description, String price, String status, String categoryName, int totalValue) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO panels (panel_name, category_id, description, price, status, created_at, category_name, total_value) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

            log.info(panelName + ", " + categoryId + ", " + description + ", " + price + ", " + status + ", " + categoryName + ", " + totalValue);
            stmt.setString(1, panelName);
            stmt.setString(2, categoryId);
            stmt.setString(3, description);
            stmt.setString(4, price);
            stmt.setString(5, status != null && !status.isEmpty() ? status : "Active");
            stmt.setString(6, LocalDateTime.now().toString());
            stmt.setString(7, categoryName);
            stmt.setInt(8, totalValue);

            int rowsAffected = stmt.executeUpdate();
            log.info("Panel added successfully: " + panelName + ", rows affected: " + rowsAffected);
            return true;

        } catch (Exception e) {
            log.severe("Failed to add panel: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static boolean updatePanel(int panelId, String panelName, String categoryId, String description, String price, String status, String categoryName, int totalValue) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE panels SET panel_name = ?, category_id = ?, description = ?, price = ?, status = ?, category_name = ?, total_value = ? WHERE panel_id = ?");

            stmt.setString(1, panelName);
            stmt.setInt(2, Integer.parseInt(categoryId));
            stmt.setString(3, description);
            stmt.setString(4, price);
            stmt.setString(5, status != null && !status.isEmpty() ? status : "Active");
            stmt.setString(6, categoryName);
            stmt.setInt(7, totalValue);
            stmt.setInt(8, panelId);

            int rowsAffected = stmt.executeUpdate();
            log.info("Panel updated successfully: id=" + panelId + ", rows affected: " + rowsAffected);
            return true;

        } catch (Exception e) {
            log.severe("Failed to update panel: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static boolean deletePanel(int panelId) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement("DELETE FROM panels WHERE panel_id = ?");
            stmt.setInt(1, panelId);

            int rowsAffected = stmt.executeUpdate();
            log.info("Panel deleted successfully: id=" + panelId + ", rows affected: " + rowsAffected);
            return true;

        } catch (Exception e) {
            log.severe("Failed to delete panel: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> getPanelByName(String panelName) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT p.panel_id, p.panel_name, p.category_id, p.description, p.price, p.status, c.category_name " +
                    "FROM panels p " +
                    "LEFT JOIN categories c ON p.category_id = c.category_id " +
                    "WHERE p.panel_name = ?");

            stmt.setString(1, panelName);
            ResultSet rs = stmt.executeQuery();

            Map<String, String> panel = new HashMap<>();
            if (rs.next()) {
                panel.put("id", String.valueOf(rs.getInt("panel_id")));
                panel.put("panel_id", String.valueOf(rs.getInt("panel_id")));
                panel.put("panel_name", rs.getString("panel_name"));
                panel.put("category_id", String.valueOf(rs.getInt("category_id")));
                panel.put("category_name", rs.getString("category_name"));
                panel.put("description", rs.getString("description"));
                panel.put("price", rs.getString("price"));
                panel.put("status", rs.getString("status"));
            }

            return panel;

        } catch (Exception e) {
            log.severe("Failed to fetch panel by name: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static List<Map<String, String>> getComponentsByPanel(int panelId) {
        try {
            List<Map<String, String>> components = new ArrayList<>();
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement("SELECT component_name, unit, normal_range, remarks, ageRange, gender, status " +
                 "FROM components WHERE panel_id = ?");
            stmt.setInt(1, panelId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, String> component = new HashMap<>();
                component.put("component_name", rs.getString("component_name"));
                component.put("unit", rs.getString("unit"));
                component.put("normal_range", rs.getString("normal_range"));
                component.put("remarks", rs.getString("remarks"));
                component.put("ageRange", rs.getString("ageRange"));
                component.put("gender", rs.getString("gender"));
                component.put("status", rs.getString("status"));
                components.add(component);
            }

            log.info("Fetched components for panel ID: " + panelId + ", count: " + components.size());
            return components;

        } catch (Exception e) {
            log.severe("Failed to fetch components for panel ID: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static List<Map<String, String>> getAllCategories() {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT id, category_name, description, status FROM categories ORDER BY category_name");

            ResultSet rs = stmt.executeQuery();

            List<Map<String, String>> categories = new ArrayList<>();
            while (rs.next()) {
                Map<String, String> category = new HashMap<>();
                category.put("id", String.valueOf(rs.getInt("id")));
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

    public static boolean addComponent(String panelName, String componentName, String ageRange, String gender, 
                                       String unit, String normalRange, String remarks, String status, int panelId) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO components (panel_name, component_name, ageRange, gender, unit, normal_range, remarks, status, panel_id, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            stmt.setString(1, panelName);
            stmt.setString(2, componentName);
            stmt.setString(3, ageRange);
            stmt.setString(4, gender);
            stmt.setString(5, unit);
            stmt.setString(6, normalRange);
            stmt.setString(7, remarks);
            stmt.setString(8, status != null && !status.isEmpty() ? status : "Active");
            stmt.setInt(9, panelId);
            stmt.setString(10, LocalDateTime.now().toString());

            int rowsAffected = stmt.executeUpdate();
            log.info("Component added successfully: " + componentName + " for panel: " + panelName + ", rows affected: " + rowsAffected);
            return true;

        } catch (Exception e) {
            log.severe("Failed to add component: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static Double getCommissions(int doctorId) {
        // Implementation for fetching commissions
        Connection conn = DatabaseManager.getConnection();
        
        String sql = "SELECT commission_percent " +
                     "FROM referring_doctors " +
                     "WHERE doctor_id = ?";
        log.info("Executing SQL to fetch commissions for doctor_id=" + doctorId + ": " + sql);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, doctorId);
            // stmt.setInt(2, panelId); // Remove this line if not needed

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("commission_percent");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }
}
