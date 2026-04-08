package labreport.service;

import labreport.model.SettlementReport;
import labreport.model.DoctorCommission;
import labreport.db.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SettlementService {
    private DatabaseManager dbManager;

    public SettlementService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Get all settlement reports
     */
    public List<SettlementReport> getAllSettlements() {
        List<SettlementReport> settlements = new ArrayList<>();
        String sql = "SELECT * FROM settlement_reports ORDER BY start_date DESC";
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                settlements.add(mapRowToSettlementReport(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return settlements;
    }

    /**
     * Get settlement report by ID
     */
    public SettlementReport getSettlementById(int settlementId) {
        String sql = "SELECT * FROM settlement_reports WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, settlementId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToSettlementReport(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get settlements for a specific doctor
     */
    public List<SettlementReport> getSettlementsByDoctor(int doctorId) {
        List<SettlementReport> settlements = new ArrayList<>();
        String sql = "SELECT * FROM settlement_reports WHERE doctor_id = ? ORDER BY start_date DESC";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, doctorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    settlements.add(mapRowToSettlementReport(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return settlements;
    }

    /**
     * Get settlements by date range
     */
    public List<SettlementReport> getSettlementsByDateRange(String startDate, String endDate) {
        List<SettlementReport> settlements = new ArrayList<>();
        String sql = "SELECT * FROM settlement_reports WHERE start_date >= ? AND end_date <= ? " +
                     "ORDER BY start_date DESC";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    settlements.add(mapRowToSettlementReport(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return settlements;
    }

    /**
     * Create a settlement report
     */
    public int createSettlement(SettlementReport settlement) {
        String sql = "INSERT INTO settlement_reports (doctor_id, start_date, end_date, " +
                     "total_reports_count, subtotal, gst_amount, total_amount, payment_status, " +
                     "payment_date, payment_method, bank_details, notes, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, settlement.getDoctorId()); // Null-safe for all doctors
            pstmt.setString(2, settlement.getStartDate());
            pstmt.setString(3, settlement.getEndDate());
            pstmt.setInt(4, settlement.getTotalReportsCount());
            pstmt.setDouble(5, settlement.getSubtotal());
            pstmt.setDouble(6, settlement.getGstAmount());
            pstmt.setDouble(7, settlement.getTotalAmount());
            pstmt.setString(8, settlement.getPaymentStatus());
            pstmt.setString(9, settlement.getPaymentDate());
            pstmt.setString(10, settlement.getPaymentMethod());
            pstmt.setString(11, settlement.getBankDetails());
            pstmt.setString(12, settlement.getNotes());
            pstmt.setLong(13, settlement.getCreatedAt());
            pstmt.setLong(14, System.currentTimeMillis());
            
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
     * Update a settlement report
     */
    public boolean updateSettlement(SettlementReport settlement) {
        String sql = "UPDATE settlement_reports SET doctor_id = ?, start_date = ?, end_date = ?, " +
                     "total_reports_count = ?, subtotal = ?, gst_amount = ?, total_amount = ?, " +
                     "payment_status = ?, payment_date = ?, payment_method = ?, bank_details = ?, " +
                     "notes = ?, updated_at = ? WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, settlement.getDoctorId());
            pstmt.setString(2, settlement.getStartDate());
            pstmt.setString(3, settlement.getEndDate());
            pstmt.setInt(4, settlement.getTotalReportsCount());
            pstmt.setDouble(5, settlement.getSubtotal());
            pstmt.setDouble(6, settlement.getGstAmount());
            pstmt.setDouble(7, settlement.getTotalAmount());
            pstmt.setString(8, settlement.getPaymentStatus());
            pstmt.setString(9, settlement.getPaymentDate());
            pstmt.setString(10, settlement.getPaymentMethod());
            pstmt.setString(11, settlement.getBankDetails());
            pstmt.setString(12, settlement.getNotes());
            pstmt.setLong(13, System.currentTimeMillis());
            pstmt.setInt(14, settlement.getId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Delete a settlement
     */
    public boolean deleteSettlement(int settlementId) {
        String sql = "DELETE FROM settlement_reports WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, settlementId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Calculate settlement for a doctor in a date range
     */
    public SettlementReport calculateSettlement(Integer doctorId, String startDate, String endDate, 
                                                Double gstRate, LabProfileService labService) {
        SettlementReport settlement = new SettlementReport();
        settlement.setDoctorId(doctorId);
        settlement.setStartDate(startDate);
        settlement.setEndDate(endDate);
        
        try (Connection conn = dbManager.getConnection()) {
            // Get total amount from reports for this doctor in date range
            String reportsSql = doctorId != null ?
                "SELECT COUNT(*) as count, SUM(total_amount) as total FROM reports " +
                "WHERE referred_by_doctor_id = ? AND report_date >= ? AND report_date <= ?" :
                "SELECT COUNT(*) as count, SUM(total_amount) as total FROM reports " +
                "WHERE report_date >= ? AND report_date <= ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(reportsSql)) {
                int paramIdx = 1;
                if (doctorId != null) {
                    pstmt.setInt(paramIdx++, doctorId);
                }
                pstmt.setString(paramIdx++, startDate);
                pstmt.setString(paramIdx, endDate);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        settlement.setTotalReportsCount(rs.getInt("count"));
                        Double subtotal = rs.getDouble("total");
                        if (subtotal == null || subtotal == 0) {
                            subtotal = 0.0;
                        }
                        settlement.setSubtotal(subtotal);
                        
                        // Calculate GST
                        Double gst = subtotal * (gstRate / 100.0);
                        settlement.setGstAmount(gst);
                        settlement.setTotalAmount(subtotal + gst);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return settlement;
    }

    // ============ DOCTOR COMMISSION METHODS ============

    /**
     * Get all doctor commissions
     */
    public List<DoctorCommission> getAllCommissions() {
        List<DoctorCommission> commissions = new ArrayList<>();
        String sql = "SELECT * FROM doctor_commissions WHERE status = 'Active' ORDER BY doctor_id ASC";
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                commissions.add(mapRowToDoctorCommission(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return commissions;
    }

    /**
     * Get commissions for a doctor
     */
    public List<DoctorCommission> getCommissionsByDoctor(int doctorId) {
        List<DoctorCommission> commissions = new ArrayList<>();
        String sql = "SELECT * FROM doctor_commissions WHERE doctor_id = ? AND status = 'Active' " +
                     "ORDER BY test_id ASC";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, doctorId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    commissions.add(mapRowToDoctorCommission(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return commissions;
    }

    /**
     * Get commission for a specific doctor/test combination
     */
    public DoctorCommission getCommission(int doctorId, Integer testId, Integer subTestId) {
        String sql = "SELECT * FROM doctor_commissions " +
                     "WHERE doctor_id = ? AND test_id = ? AND sub_test_id = ? AND status = 'Active'";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, doctorId);
            pstmt.setObject(2, testId);
            pstmt.setObject(3, subTestId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToDoctorCommission(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Create a doctor commission
     */
    public int createCommission(DoctorCommission commission) {
        String sql = "INSERT INTO doctor_commissions (doctor_id, test_id, sub_test_id, " +
                     "commission_rate, commission_type, fixed_amount, status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, commission.getDoctorId());
            pstmt.setObject(2, commission.getTestId()); // Null-safe
            pstmt.setObject(3, commission.getSubTestId()); // Null-safe
            pstmt.setDouble(4, commission.getCommissionRate());
            pstmt.setString(5, commission.getCommissionType().name());
            pstmt.setObject(6, commission.getFixedAmount()); // Null-safe
            pstmt.setString(7, commission.getStatus());
            pstmt.setLong(8, commission.getCreatedAt());
            pstmt.setLong(9, System.currentTimeMillis());
            
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
     * Update a doctor commission
     */
    public boolean updateCommission(DoctorCommission commission) {
        String sql = "UPDATE doctor_commissions SET commission_rate = ?, commission_type = ?, " +
                     "fixed_amount = ?, status = ?, updated_at = ? WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, commission.getCommissionRate());
            pstmt.setString(2, commission.getCommissionType().name());
            pstmt.setObject(3, commission.getFixedAmount());
            pstmt.setString(4, commission.getStatus());
            pstmt.setLong(5, System.currentTimeMillis());
            pstmt.setInt(6, commission.getId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Delete a doctor commission
     */
    public boolean deleteCommission(int commissionId) {
        String sql = "UPDATE doctor_commissions SET status = 'Inactive', updated_at = ? WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis());
            pstmt.setInt(2, commissionId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ============ HELPER METHODS ============

    private SettlementReport mapRowToSettlementReport(ResultSet rs) throws SQLException {
        SettlementReport settlement = new SettlementReport();
        settlement.setId(rs.getInt("id"));
        
        Object doctorId = rs.getObject("doctor_id");
        settlement.setDoctorId(doctorId != null ? rs.getInt("doctor_id") : null);
        
        settlement.setStartDate(rs.getString("start_date"));
        settlement.setEndDate(rs.getString("end_date"));
        settlement.setTotalReportsCount(rs.getInt("total_reports_count"));
        settlement.setSubtotal(rs.getDouble("subtotal"));
        settlement.setGstAmount(rs.getDouble("gst_amount"));
        settlement.setTotalAmount(rs.getDouble("total_amount"));
        settlement.setPaymentStatus(rs.getString("payment_status"));
        settlement.setPaymentDate(rs.getString("payment_date"));
        settlement.setPaymentMethod(rs.getString("payment_method"));
        settlement.setBankDetails(rs.getString("bank_details"));
        settlement.setNotes(rs.getString("notes"));
        settlement.setCreatedAt(rs.getLong("created_at"));
        settlement.setUpdatedAt(rs.getLong("updated_at"));
        return settlement;
    }

    private DoctorCommission mapRowToDoctorCommission(ResultSet rs) throws SQLException {
        DoctorCommission commission = new DoctorCommission();
        commission.setId(rs.getInt("id"));
        commission.setDoctorId(rs.getInt("doctor_id"));
        
        Object testId = rs.getObject("test_id");
        commission.setTestId(testId != null ? rs.getInt("test_id") : null);
        
        Object subTestId = rs.getObject("sub_test_id");
        commission.setSubTestId(subTestId != null ? rs.getInt("sub_test_id") : null);
        
        commission.setCommissionRate(rs.getDouble("commission_rate"));
        String commissionTypeStr = rs.getString("commission_type");
        commission.setCommissionType(DoctorCommission.DoctorCommissionType.valueOf(commissionTypeStr));
        commission.setFixedAmount(rs.getDouble("fixed_amount"));
        commission.setStatus(rs.getString("status"));
        commission.setCreatedAt(rs.getLong("created_at"));
        commission.setUpdatedAt(rs.getLong("updated_at"));
        return commission;
    }
}
