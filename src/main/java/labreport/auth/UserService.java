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

            // 1. Check if any user exists
            PreparedStatement checkStmt =
                    conn.prepareStatement("SELECT COUNT(*) FROM users");
            ResultSet rs = checkStmt.executeQuery();

            int userCount = rs.next() ? rs.getInt(1) : 0;

            if (userCount > 0) {
                log.info("Users already exist. Default admin setup skipped.");
                return;
            }

            // 2. Insert default admin
            String hashedPassword = PasswordHasher.hash("admin786");

            PreparedStatement insertStmt =
                    conn.prepareStatement(
                            "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)");

            insertStmt.setString(1, "admin");
            insertStmt.setString(2, hashedPassword);
            insertStmt.setString(3, "ADMIN");

            insertStmt.executeUpdate();

            log.info("Default admin user created (username=admin)");

        } catch (Exception e) {
            log.severe("Failed to setup default admin user");
            log.severe(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void ensureDefaultLabProfile() {
        try {
            Connection conn = DatabaseManager.getConnection();

            // Check if lab profile with lab_id=1 exists
            PreparedStatement checkStmt =
                    conn.prepareStatement("SELECT COUNT(*) FROM LabProfile WHERE lab_id = 1");
            ResultSet rs = checkStmt.executeQuery();

            int profileCount = rs.next() ? rs.getInt(1) : 0;

            if (profileCount > 0) {
                log.info("Lab profile for lab_id=1 already exists. Setup skipped.");
                return;
            }

            // Insert default lab profile
            PreparedStatement insertStmt =
                    conn.prepareStatement(
                            "INSERT INTO LabProfile (lab_id, lab_name, address, contact_number, updated_at) VALUES (?, ?, ?, ?, ?)");

            insertStmt.setInt(1, 1);
            insertStmt.setString(2, "Bharat Pathology Laboratory");
            insertStmt.setString(3, "123 Medical Street, Health City");
            insertStmt.setString(4, "+91-1234567890");
            insertStmt.setString(5, java.time.LocalDateTime.now().toString());

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

}
