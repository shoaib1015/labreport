package labreport.service;

import labreport.model.Doctor;
import labreport.db.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorService {
    private DatabaseManager dbManager;

    public DoctorService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Get all doctors
     */
    public List<Doctor> getAllDoctors() {
        List<Doctor> doctors = new ArrayList<>();
        String sql = "SELECT * FROM doctors WHERE status = 'Active' ORDER BY doctor_name ASC";
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                doctors.add(mapRowToDoctor(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return doctors;
    }

    /**
     * Get doctor by ID
     */
    public Doctor getDoctorById(int doctorId) {
        String sql = "SELECT * FROM doctors WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, doctorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToDoctor(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Search doctors by name
     */
    public List<Doctor> searchDoctorsByName(String name) {
        List<Doctor> doctors = new ArrayList<>();
        String sql = "SELECT * FROM doctors WHERE (doctor_name LIKE ? OR clinic_name LIKE ?) " +
                     "AND status = 'Active' ORDER BY doctor_name ASC";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String searchTerm = "%" + name + "%";
            pstmt.setString(1, searchTerm);
            pstmt.setString(2, searchTerm);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    doctors.add(mapRowToDoctor(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return doctors;
    }

    /**
     * Create a new doctor
     */
    public int createDoctor(Doctor doctor) {
        String sql = "INSERT INTO doctors (doctor_name, specialization, qualification, experience, " +
                     "registration_number, primary_phone, secondary_phone, email, fax, " +
                     "clinic_name, clinic_address, clinic_city, clinic_state, clinic_phone, clinic_email, " +
                     "default_commission_rate, settlement_type, notes, status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setDoctorParams(pstmt, doctor);
            
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
     * Update an existing doctor
     */
    public boolean updateDoctor(Doctor doctor) {
        String sql = "UPDATE doctors SET doctor_name = ?, specialization = ?, qualification = ?, " +
                     "experience = ?, registration_number = ?, primary_phone = ?, secondary_phone = ?, " +
                     "email = ?, fax = ?, clinic_name = ?, clinic_address = ?, clinic_city = ?, " +
                     "clinic_state = ?, clinic_phone = ?, clinic_email = ?, default_commission_rate = ?, " +
                     "settlement_type = ?, notes = ?, status = ?, updated_at = ? WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, doctor.getDoctorName());
            pstmt.setString(2, doctor.getSpecialization());
            pstmt.setString(3, doctor.getQualification());
            if (doctor.getExperience() != null) {
                pstmt.setInt(4, doctor.getExperience());
            } else {
                pstmt.setNull(4, Types.INTEGER);
            }
            pstmt.setString(5, doctor.getRegistrationNumber());
            pstmt.setString(6, doctor.getPrimaryPhone());
            pstmt.setString(7, doctor.getSecondaryPhone());
            pstmt.setString(8, doctor.getEmail());
            pstmt.setString(9, doctor.getFax());
            pstmt.setString(10, doctor.getClinicName());
            pstmt.setString(11, doctor.getClinicAddress());
            pstmt.setString(12, doctor.getClinicCity());
            pstmt.setString(13, doctor.getClinicState());
            pstmt.setString(14, doctor.getClinicPhone());
            pstmt.setString(15, doctor.getClinicEmail());
            pstmt.setDouble(16, doctor.getDefaultCommissionRate());
            pstmt.setString(17, doctor.getSettlementType());
            pstmt.setString(18, doctor.getNotes());
            pstmt.setString(19, doctor.getStatus());
            pstmt.setLong(20, System.currentTimeMillis());
            pstmt.setInt(21, doctor.getId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Delete a doctor (soft delete - mark as inactive)
     */
    public boolean deleteDoctor(int doctorId) {
        String sql = "UPDATE doctors SET status = 'Inactive', updated_at = ? WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis());
            pstmt.setInt(2, doctorId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Helper method to set parameters for doctor prepared statements
     */
    private void setDoctorParams(PreparedStatement pstmt, Doctor doctor) throws SQLException {
        pstmt.setString(1, doctor.getDoctorName());
        pstmt.setString(2, doctor.getSpecialization());
        pstmt.setString(3, doctor.getQualification());
        if (doctor.getExperience() != null) {
            pstmt.setInt(4, doctor.getExperience());
        } else {
            pstmt.setNull(4, Types.INTEGER);
        }
        pstmt.setString(5, doctor.getRegistrationNumber());
        pstmt.setString(6, doctor.getPrimaryPhone());
        pstmt.setString(7, doctor.getSecondaryPhone());
        pstmt.setString(8, doctor.getEmail());
        pstmt.setString(9, doctor.getFax());
        pstmt.setString(10, doctor.getClinicName());
        pstmt.setString(11, doctor.getClinicAddress());
        pstmt.setString(12, doctor.getClinicCity());
        pstmt.setString(13, doctor.getClinicState());
        pstmt.setString(14, doctor.getClinicPhone());
        pstmt.setString(15, doctor.getClinicEmail());
        pstmt.setDouble(16, doctor.getDefaultCommissionRate());
        pstmt.setString(17, doctor.getSettlementType());
        pstmt.setString(18, doctor.getNotes());
        pstmt.setString(19, doctor.getStatus());
        pstmt.setLong(20, doctor.getCreatedAt());
        pstmt.setLong(21, System.currentTimeMillis());
    }

    /**
     * Helper method to map a ResultSet row to a Doctor object
     */
    private Doctor mapRowToDoctor(ResultSet rs) throws SQLException {
        Doctor doctor = new Doctor();
        doctor.setId(rs.getInt("id"));
        doctor.setDoctorName(rs.getString("doctor_name"));
        doctor.setSpecialization(rs.getString("specialization"));
        doctor.setQualification(rs.getString("qualification"));
        Object experienceObj = rs.getObject("experience");
        doctor.setExperience(experienceObj != null ? rs.getInt("experience") : null);
        doctor.setRegistrationNumber(rs.getString("registration_number"));
        doctor.setPrimaryPhone(rs.getString("primary_phone"));
        doctor.setSecondaryPhone(rs.getString("secondary_phone"));
        doctor.setEmail(rs.getString("email"));
        doctor.setFax(rs.getString("fax"));
        doctor.setClinicName(rs.getString("clinic_name"));
        doctor.setClinicAddress(rs.getString("clinic_address"));
        doctor.setClinicCity(rs.getString("clinic_city"));
        doctor.setClinicState(rs.getString("clinic_state"));
        doctor.setClinicPhone(rs.getString("clinic_phone"));
        doctor.setClinicEmail(rs.getString("clinic_email"));
        doctor.setDefaultCommissionRate(rs.getDouble("default_commission_rate"));
        doctor.setSettlementType(rs.getString("settlement_type"));
        doctor.setNotes(rs.getString("notes"));
        doctor.setStatus(rs.getString("status"));
        doctor.setCreatedAt(rs.getLong("created_at"));
        doctor.setUpdatedAt(rs.getLong("updated_at"));
        return doctor;
    }
}
