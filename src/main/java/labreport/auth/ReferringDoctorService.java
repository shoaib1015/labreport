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

public class ReferringDoctorService {

    private static final Logger log = AppLogger.getLogger();

    public static List<Map<String, String>> getAllDoctors() {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT doctor_id, full_name, contact_number, license_number, status, commission_percent FROM referring_doctors ORDER BY full_name");

            ResultSet rs = stmt.executeQuery();

            List<Map<String, String>> doctors = new ArrayList<>();
            while (rs.next()) {
                Map<String, String> doctor = new HashMap<>();
                doctor.put("id", String.valueOf(rs.getInt("doctor_id")));
                doctor.put("full_name", rs.getString("full_name"));
                doctor.put("contact_number", rs.getString("contact_number"));
                doctor.put("license_number", rs.getString("license_number"));
                doctor.put("status", rs.getString("status"));
                doctor.put("commission_percent", String.valueOf(rs.getDouble("commission_percent")));
                doctors.add(doctor);
            }

            log.info("Fetched " + doctors.size() + " doctors from database");
            return doctors;

        } catch (Exception e) {
            log.severe("Failed to fetch doctors: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> getDoctorById(int doctorId) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT doctor_id, full_name, contact_number, license_number, status, commission_percent FROM referring_doctors WHERE doctor_id = ?");

            stmt.setInt(1, doctorId);
            ResultSet rs = stmt.executeQuery();

            Map<String, String> doctor = new HashMap<>();
            if (rs.next()) {
                doctor.put("id", String.valueOf(rs.getInt("doctor_id")));
                doctor.put("full_name", rs.getString("full_name"));
                doctor.put("contact_number", rs.getString("contact_number"));
                doctor.put("license_number", rs.getString("license_number"));
                doctor.put("status", rs.getString("status"));
                doctor.put("commission_percent", String.valueOf(rs.getDouble("commission_percent")));
            }

            return doctor;

        } catch (Exception e) {
            log.severe("Failed to fetch doctor by id: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static boolean addDoctor(String fullName, String contactNumber, String licenseNumber, String status, double commissionPercent) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO referring_doctors (full_name, contact_number, license_number, status, commission_percent, created_at) VALUES (?, ?, ?, ?, ?, ?)");

            stmt.setString(1, fullName);
            stmt.setString(2, contactNumber);
            stmt.setString(3, licenseNumber);
            stmt.setString(4, status != null && !status.isEmpty() ? status : "Active");
            stmt.setDouble(5, commissionPercent);
            stmt.setString(6, LocalDateTime.now().toString());

            int rowsAffected = stmt.executeUpdate();
            log.info("Doctor added successfully: " + fullName + ", rows affected: " + rowsAffected);
            return true;

        } catch (Exception e) {
            log.severe("Failed to add doctor: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static boolean updateDoctor(int doctorId, String fullName, String contactNumber, String licenseNumber, String status, double commissionPercent) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE referring_doctors SET full_name = ?, contact_number = ?, license_number = ?, status = ?, commission_percent = ? WHERE doctor_id = ?");

            stmt.setString(1, fullName);
            stmt.setString(2, contactNumber);
            stmt.setString(3, licenseNumber);
            stmt.setString(4, status != null && !status.isEmpty() ? status : "Active");
            stmt.setDouble(5, commissionPercent);
            stmt.setInt(6, doctorId);

            int rowsAffected = stmt.executeUpdate();
            log.info("Doctor updated successfully: id=" + doctorId + ", rows affected: " + rowsAffected);
            return true;

        } catch (Exception e) {
            log.severe("Failed to update doctor: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static boolean deleteDoctor(int doctorId) {
        try {
            Connection conn = DatabaseManager.getConnection();

            PreparedStatement stmt = conn.prepareStatement("DELETE FROM referring_doctors WHERE doctor_id = ?");
            stmt.setInt(1, doctorId);

            int rowsAffected = stmt.executeUpdate();
            log.info("Doctor deleted successfully: id=" + doctorId + ", rows affected: " + rowsAffected);
            return true;

        } catch (Exception e) {
            log.severe("Failed to delete doctor: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
