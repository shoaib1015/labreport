package labreport.auth;

import labreport.db.DatabaseManager;
import labreport.logging.AppLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class LabProfileService {

    private static final Logger log = AppLogger.getLogger();

    public static Map<String, String> getLabProfile(int labId) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT lab_id, lab_name, address, contact_number FROM LabProfile WHERE lab_id = ?");

            stmt.setInt(1, labId);
            ResultSet rs = stmt.executeQuery();

            Map<String, String> profile = new HashMap<>();
            if (rs.next()) {
                profile.put("lab_id", String.valueOf(rs.getInt("lab_id")));
                profile.put("lab_name", rs.getString("lab_name"));
                profile.put("address", rs.getString("address"));
                profile.put("contact_number", rs.getString("contact_number"));
            }

            return profile;

        } catch (Exception e) {
            log.severe("Failed to fetch lab profile: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static boolean updateLabProfile(int labId, String labName, String address, String contactNumber) {
        try {
            Connection conn = DatabaseManager.getConnection();

            // Check if lab profile exists
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM LabProfile WHERE lab_id = ?");
            checkStmt.setInt(1, labId);
            ResultSet rs = checkStmt.executeQuery();
            boolean exists = rs.next() && rs.getInt(1) > 0;

            if (exists) {
                // Update existing record
                PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE LabProfile SET lab_name = ?, address = ?, contact_number = ?, updated_at = ? WHERE lab_id = ?");

                updateStmt.setString(1, labName);
                updateStmt.setString(2, address);
                updateStmt.setString(3, contactNumber);
                updateStmt.setString(4, LocalDateTime.now().toString());
                updateStmt.setInt(5, labId);

                int rowsAffected = updateStmt.executeUpdate();
                log.info("Lab profile updated for lab_id: " + labId + ", rows affected: " + rowsAffected);
                return true;

            } else {
                // Insert new record
                PreparedStatement insertStmt = conn.prepareStatement(
                        "INSERT INTO LabProfile (lab_id, lab_name, address, contact_number, updated_at) VALUES (?, ?, ?, ?, ?)");

                insertStmt.setInt(1, labId);
                insertStmt.setString(2, labName);
                insertStmt.setString(3, address);
                insertStmt.setString(4, contactNumber);
                insertStmt.setString(5, LocalDateTime.now().toString());

                int rowsAffected = insertStmt.executeUpdate();
                log.info("Lab profile created for lab_id: " + labId + ", rows affected: " + rowsAffected);
                return true;
            }

        } catch (Exception e) {
            log.severe("Failed to update lab profile: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
