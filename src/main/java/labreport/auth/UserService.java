package labreport.auth;

import labreport.db.DatabaseManager;
import labreport.logging.AppLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

public class UserService {

    private static final Logger log = AppLogger.getLogger();

    public static void ensureDefaultAdmin() {
        try {
            Connection conn = DatabaseManager.getConnection();

            // 1. Check if admin user exists
            PreparedStatement checkAdminStmt =
                    conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?");
            checkAdminStmt.setString(1, "admin");
            ResultSet adminRs = checkAdminStmt.executeQuery();

            int adminCount = adminRs.next() ? adminRs.getInt(1) : 0;

            if (adminCount == 0) {
                // 2. Insert default admin if it doesn't exist
                String hashedPassword = PasswordHasher.hash("admin786");

                PreparedStatement insertStmt =
                        conn.prepareStatement(
                                "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)");

                insertStmt.setString(1, "admin");
                insertStmt.setString(2, hashedPassword);
                insertStmt.setString(3, "ADMIN");

                insertStmt.executeUpdate();

                log.info("Default admin user created (username=admin)");
            } else {
                log.info("Admin user already exists. Setup skipped.");
            }

            // 3. Check if staff user exists
            PreparedStatement checkStaffStmt =
                    conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?");
            checkStaffStmt.setString(1, "staff");
            ResultSet staffRs = checkStaffStmt.executeQuery();

            int staffCount = staffRs.next() ? staffRs.getInt(1) : 0;

            if (staffCount == 0) {
                // 4. Insert default staff user if it doesn't exist
                String staffHashedPassword = PasswordHasher.hash("staff786");

                PreparedStatement staffInsertStmt =
                        conn.prepareStatement(
                                "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)");

                staffInsertStmt.setString(1, "staff");
                staffInsertStmt.setString(2, staffHashedPassword);
                staffInsertStmt.setString(3, "STAFF");

                staffInsertStmt.executeUpdate();

                log.info("Default staff user created (username=staff)");
            } else {
                log.info("Staff user already exists. Setup skipped.");
            }

        } catch (Exception e) {
            log.severe("Failed to setup default users");
            log.severe(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void ensureDefaultLabProfile() {
        try {
            Connection conn = DatabaseManager.getConnection();

            // Check if lab profile with lab_id=1 exists
            PreparedStatement checkStmt =
                    conn.prepareStatement("SELECT COUNT(*) FROM lab_profile WHERE lab_id = 1");
            ResultSet rs = checkStmt.executeQuery();

            int profileCount = rs.next() ? rs.getInt(1) : 0;

            if (profileCount > 0) {
                log.info("Lab profile for lab_id=1 already exists. Setup skipped.");
                return;
            }

            // Insert default lab profile
            PreparedStatement insertStmt =
                    conn.prepareStatement(
                            "INSERT INTO lab_profile (lab_id, lab_name, registration_number, address, contact_number, email, website, director_name, license_number, accreditation, status, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            insertStmt.setInt(1, 1);
            insertStmt.setString(2, "Bharat Pathology Laboratory");
            insertStmt.setString(3, "REG-00123");
            insertStmt.setString(4, "123 Medical Street, Health City");
            insertStmt.setString(5, "+91-1234567890");
            insertStmt.setString(6, "info@bharatpathology.com");
            insertStmt.setString(7, "www.bharatpathology.com");
            insertStmt.setString(8, "Dr. A. Sharma");
            insertStmt.setString(9, "LIC-987654");
            insertStmt.setString(10, "NABL Accredited");
            insertStmt.setString(11, "Active");
            insertStmt.setString(12, java.time.LocalDateTime.now().toString());

            insertStmt.executeUpdate();

            log.info("Default lab profile created for lab_id=1");

        } catch (Exception e) {
            log.severe("Failed to setup default lab profile");
            log.severe(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static boolean validateCredentials(String username, String password) {
    try {
        Connection conn = DatabaseManager.getConnection();

        PreparedStatement stmt =
                conn.prepareStatement(
                        "SELECT password_hash FROM users WHERE username = ?");

        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();

        if (!rs.next()) {
            return false;
        }

        String storedHash = rs.getString("password_hash");
        String inputHash = PasswordHasher.hash(password);

        return storedHash.equals(inputHash);

    } catch (Exception e) {
        throw new RuntimeException("Credential validation failed", e);
    }
}

    public static String getUserRole(String username) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt =
                    conn.prepareStatement(
                            "SELECT role FROM users WHERE username = ?");

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                return null;
            }

            return rs.getString("role");

        } catch (Exception e) {
            log.severe("Error retrieving user role: " + e.getMessage());
            throw new RuntimeException("Role retrieval failed", e);
        }
    }

}
