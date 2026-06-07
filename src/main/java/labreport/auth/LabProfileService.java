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
                    "SELECT lab_id, lab_name, registration_number, address, contact_number, email, website, director_name, license_number, accreditation, status FROM lab_profile WHERE lab_id = ?");

            stmt.setInt(1, labId);
            ResultSet rs = stmt.executeQuery();
            log.info("Result"+rs);
            Map<String, String> profile = new HashMap<>();
            if (rs.next()) {
                log.info("Lab profile found for lab_id: " + labId);
                profile.put("lab_id", String.valueOf(rs.getInt("lab_id")));
                profile.put("lab_name", rs.getString("lab_name"));
                profile.put("registration_number", rs.getString("registration_number"));
                profile.put("address", rs.getString("address"));
                profile.put("contact_number", rs.getString("contact_number"));
                profile.put("email", rs.getString("email"));
                profile.put("website", rs.getString("website"));
                profile.put("director_name", rs.getString("director_name"));
                profile.put("license_number", rs.getString("license_number"));
                profile.put("accreditation", rs.getString("accreditation"));
                profile.put("status", rs.getString("status"));
            }
            log.info("Lab profile fetched for lab_id: " + labId + ", profile: " + profile);
            return profile;

        } catch (Exception e) {
            log.severe("Failed to fetch lab profile: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static boolean updateLabProfile(int labId, String labName, String registrationNumber, String address,
            String contactNumber, String email, String website, String directorName, String licenseNumber,
            String accreditation, String status) {
        try {
            Connection conn = DatabaseManager.getConnection();

            // Check if lab profile exists
            PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM lab_profile WHERE lab_id = ?");
            checkStmt.setInt(1, labId);
            ResultSet rs = checkStmt.executeQuery();
            boolean exists = rs.next() && rs.getInt(1) > 0;

            if (exists) {
                // Update existing record
                PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE lab_profile SET lab_name = ?, registration_number = ?, address = ?, contact_number = ?, email = ?, website = ?, director_name = ?, license_number = ?, accreditation = ?, status = ?, updated_at = ? WHERE lab_id = ?");

                updateStmt.setString(1, labName);
                updateStmt.setString(2, registrationNumber);
                updateStmt.setString(3, address);
                updateStmt.setString(4, contactNumber);
                updateStmt.setString(5, email);
                updateStmt.setString(6, website);
                updateStmt.setString(7, directorName);
                updateStmt.setString(8, licenseNumber);
                updateStmt.setString(9, accreditation);
                updateStmt.setString(10, status);
                updateStmt.setString(11, LocalDateTime.now().toString());
                updateStmt.setInt(12, labId);

                int rowsAffected = updateStmt.executeUpdate();
                log.info("Lab profile updated for lab_id: " + labId + ", rows affected: " + rowsAffected);
                return true;

            } else {
                // Insert new record
                PreparedStatement insertStmt = conn.prepareStatement(
                        "INSERT INTO lab_profile (lab_id, lab_name, registration_number, address, contact_number, email, website, director_name, license_number, accreditation, status, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

                insertStmt.setInt(1, labId);
                insertStmt.setString(2, labName);
                insertStmt.setString(3, registrationNumber);
                insertStmt.setString(4, address);
                insertStmt.setString(5, contactNumber);
                insertStmt.setString(6, email);
                insertStmt.setString(7, website);
                insertStmt.setString(8, directorName);
                insertStmt.setString(9, licenseNumber);
                insertStmt.setString(10, accreditation);
                insertStmt.setString(11, status);
                insertStmt.setString(12, LocalDateTime.now().toString());

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
