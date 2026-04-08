package labreport.service;

import labreport.model.Patient;
import labreport.db.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientService {
    private DatabaseManager dbManager;

    public PatientService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Get all patients
     */
    public List<Patient> getAllPatients() {
        List<Patient> patients = new ArrayList<>();
        String sql = "SELECT * FROM patients WHERE status = 'Active' ORDER BY patient_name ASC";
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                patients.add(mapRowToPatient(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return patients;
    }

    /**
     * Get patient by ID
     */
    public Patient getPatientById(int patientId) {
        String sql = "SELECT * FROM patients WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, patientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPatient(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get patient by patientId (auto-generated ID like 26-15C1)
     */
    public Patient getPatientByPatientId(String patientId) {
        String sql = "SELECT * FROM patients WHERE patient_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, patientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPatient(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Search patients by name
     */
    public List<Patient> searchByName(String patientName) {
        List<Patient> patients = new ArrayList<>();
        String sql = "SELECT * FROM patients WHERE (patient_name LIKE ? OR patient_id LIKE ?) " +
                     "AND status = 'Active' ORDER BY patient_name ASC";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String searchTerm = "%" + patientName + "%";
            pstmt.setString(1, searchTerm);
            pstmt.setString(2, searchTerm);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    patients.add(mapRowToPatient(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return patients;
    }

    /**
     * Search patients by phone
     */
    public List<Patient> searchByPhone(String phone) {
        List<Patient> patients = new ArrayList<>();
        String sql = "SELECT * FROM patients WHERE phone LIKE ? AND status = 'Active' ORDER BY patient_name ASC";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + phone + "%");
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    patients.add(mapRowToPatient(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return patients;
    }

    /**
     * Search patients by referred doctor
     */
    public List<Patient> searchByReferredDoctor(int doctorId) {
        List<Patient> patients = new ArrayList<>();
        String sql = "SELECT * FROM patients WHERE referred_by_doctor_id = ? AND status = 'Active' " +
                     "ORDER BY patient_name ASC";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, doctorId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    patients.add(mapRowToPatient(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return patients;
    }

    /**
     * Create a new patient
     */
    public int createPatient(Patient patient) {
        String sql = "INSERT INTO patients (patient_id, patient_name, date_of_birth, age, gender, phone, email, " +
                     "street_address, city, state, postal_code, country, referred_by_doctor_id, referral_doctor, " +
                     "medical_history, allergies, medications, status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setPatientParams(pstmt, patient);
            
            if (pstmt.executeUpdate() > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Update a patient
     */
    public boolean updatePatient(Patient patient) {
        String sql = "UPDATE patients SET patient_name = ?, date_of_birth = ?, age = ?, gender = ?, " +
                     "phone = ?, email = ?, street_address = ?, city = ?, state = ?, postal_code = ?, " +
                     "country = ?, referred_by_doctor_id = ?, referral_doctor = ?, medical_history = ?, " +
                     "allergies = ?, medications = ?, status = ?, updated_at = ? WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, patient.getPatientName());
            pstmt.setString(2, patient.getDateOfBirth());
            pstmt.setInt(3, patient.getAge());
            pstmt.setString(4, patient.getGender());
            pstmt.setString(5, patient.getPhone());
            pstmt.setString(6, patient.getEmail());
            pstmt.setString(7, patient.getStreetAddress());
            pstmt.setString(8, patient.getCity());
            pstmt.setString(9, patient.getState());
            pstmt.setString(10, patient.getPostalCode());
            pstmt.setString(11, patient.getCountry());
            pstmt.setObject(12, patient.getReferredByDoctorId()); // Null-safe
            pstmt.setString(13, patient.getReferralDoctor());
            pstmt.setString(14, patient.getMedicalHistory());
            pstmt.setString(15, patient.getAllergies());
            pstmt.setString(16, patient.getMedications());
            pstmt.setString(17, patient.getStatus());
            pstmt.setLong(18, System.currentTimeMillis());
            pstmt.setInt(19, patient.getId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Delete a patient (soft delete)
     */
    public boolean deletePatient(int patientId) {
        String sql = "UPDATE patients SET status = 'Inactive', updated_at = ? WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis());
            pstmt.setInt(2, patientId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Generate next patient ID
     */
    public String generatePatientId() {
        // Format: YY-XXXN (e.g., 26-15C1 for 2026, sequence 15, position C, occurrence 1)
        // For now, simple implementation - can be enhanced
        int year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        String yy = String.valueOf(year).substring(2);
        
        String lastPatientIdSql = "SELECT patient_id FROM patients ORDER BY id DESC LIMIT 1";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(lastPatientIdSql)) {
            if (rs.next()) {
                String lastId = rs.getString("patient_id");
                // Extract sequence number and increment
                if (lastId != null && lastId.startsWith(yy)) {
                    // Simple increment: just append sequence
                    String[] parts = lastId.split("-");
                    if (parts.length > 1) {
                        int sequence = Integer.parseInt(parts[1].replaceAll("[^0-9]", "")) + 1;
                        return yy + "-" + String.format("%03d", sequence) + "A1";
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Default first patient ID
        return yy + "-001A1";
    }

    /**
     * Helper method to set parameters for patient prepared statements
     */
    private void setPatientParams(PreparedStatement pstmt, Patient patient) throws SQLException {
        pstmt.setString(1, patient.getPatientId());
        pstmt.setString(2, patient.getPatientName());
        pstmt.setString(3, patient.getDateOfBirth());
        pstmt.setInt(4, patient.getAge());
        pstmt.setString(5, patient.getGender());
        pstmt.setString(6, patient.getPhone());
        pstmt.setString(7, patient.getEmail());
        pstmt.setString(8, patient.getStreetAddress());
        pstmt.setString(9, patient.getCity());
        pstmt.setString(10, patient.getState());
        pstmt.setString(11, patient.getPostalCode());
        pstmt.setString(12, patient.getCountry());
        pstmt.setObject(13, patient.getReferredByDoctorId());
        pstmt.setString(14, patient.getReferralDoctor());
        pstmt.setString(15, patient.getMedicalHistory());
        pstmt.setString(16, patient.getAllergies());
        pstmt.setString(17, patient.getMedications());
        pstmt.setString(18, patient.getStatus());
        pstmt.setLong(19, patient.getCreatedAt());
        pstmt.setLong(20, System.currentTimeMillis());
    }

    /**
     * Helper method to map ResultSet row to Patient object
     */
    private Patient mapRowToPatient(ResultSet rs) throws SQLException {
        Patient patient = new Patient();
        patient.setId(rs.getInt("id"));
        patient.setPatientId(rs.getString("patient_id"));
        patient.setPatientName(rs.getString("patient_name"));
        patient.setDateOfBirth(rs.getString("date_of_birth"));
        patient.setAge(rs.getInt("age"));
        patient.setGender(rs.getString("gender"));
        patient.setPhone(rs.getString("phone"));
        patient.setEmail(rs.getString("email"));
        patient.setStreetAddress(rs.getString("street_address"));
        patient.setCity(rs.getString("city"));
        patient.setState(rs.getString("state"));
        patient.setPostalCode(rs.getString("postal_code"));
        patient.setCountry(rs.getString("country"));
        
        Object doctorId = rs.getObject("referred_by_doctor_id");
        patient.setReferredByDoctorId(doctorId != null ? rs.getInt("referred_by_doctor_id") : null);
        
        patient.setReferralDoctor(rs.getString("referral_doctor"));
        patient.setMedicalHistory(rs.getString("medical_history"));
        patient.setAllergies(rs.getString("allergies"));
        patient.setMedications(rs.getString("medications"));
        patient.setStatus(rs.getString("status"));
        patient.setCreatedAt(rs.getLong("created_at"));
        patient.setUpdatedAt(rs.getLong("updated_at"));
        return patient;
    }
}
