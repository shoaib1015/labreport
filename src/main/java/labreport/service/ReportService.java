package labreport.service;

import labreport.model.Report;
import labreport.model.ReportTestResult;
import labreport.db.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportService {
    private DatabaseManager dbManager;

    public ReportService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // ============ REPORT METHODS ============

    /**
     * Get all reports
     */
    public List<Report> getAllReports() {
        List<Report> reports = new ArrayList<>();
        String sql = "SELECT * FROM reports WHERE status != 'Deleted' ORDER BY report_date DESC";
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Report report = mapRowToReport(rs);
                // Load test results
                report.setTestResults(getReportTestResults(conn, report.getId()));
                reports.add(report);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reports;
    }

    /**
     * Get report by ID
     */
    public Report getReportById(int reportId) {
        String sql = "SELECT * FROM reports WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, reportId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Report report = mapRowToReport(rs);
                    // Load test results
                    report.setTestResults(getReportTestResults(conn, reportId));
                    return report;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get reports for a patient
     */
    public List<Report> getReportsByPatientId(int patientId) {
        List<Report> reports = new ArrayList<>();
        String sql = "SELECT * FROM reports WHERE patient_id = ? AND status != 'Deleted' " +
                     "ORDER BY report_date DESC";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, patientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Report report = mapRowToReport(rs);
                    report.setTestResults(getReportTestResults(conn, report.getId()));
                    reports.add(report);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reports;
    }

    /**
     * Get reports by doctor
     */
    public List<Report> getReportsByDoctorId(int doctorId) {
        List<Report> reports = new ArrayList<>();
        String sql = "SELECT * FROM reports WHERE referred_by_doctor_id = ? AND status != 'Deleted' " +
                     "ORDER BY report_date DESC";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, doctorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Report report = mapRowToReport(rs);
                    report.setTestResults(getReportTestResults(conn, report.getId()));
                    reports.add(report);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reports;
    }

    /**
     * Get reports by date range
     */
    public List<Report> getReportsByDateRange(String startDate, String endDate) {
        List<Report> reports = new ArrayList<>();
        String sql = "SELECT * FROM reports WHERE report_date >= ? AND report_date <= ? " +
                     "AND status != 'Deleted' ORDER BY report_date DESC";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Report report = mapRowToReport(rs);
                    report.setTestResults(getReportTestResults(conn, report.getId()));
                    reports.add(report);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reports;
    }

    /**
     * Create a new report
     */
    public int createReport(Report report) {
        String sql = "INSERT INTO reports (patient_id, report_date, referred_by_doctor_id, clinical_notes, " +
                     "sample_collection_date, report_submitted_date, testing_by, approved_by, status, " +
                     "file_path, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, report.getPatientId());
            pstmt.setString(2, report.getReportDate());
            pstmt.setObject(3, report.getReferredByDoctorId()); // Null-safe
            pstmt.setString(4, report.getClinicalNotes());
            pstmt.setString(5, report.getSampleCollectionDate());
            pstmt.setString(6, report.getReportSubmittedDate());
            pstmt.setString(7, report.getTestingBy());
            pstmt.setString(8, report.getApprovedBy());
            pstmt.setString(9, report.getStatus());
            pstmt.setString(10, report.getFilePath());
            pstmt.setLong(11, report.getCreatedAt());
            pstmt.setLong(12, System.currentTimeMillis());
            
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
     * Update a report
     */
    public boolean updateReport(Report report) {
        String sql = "UPDATE reports SET patient_id = ?, report_date = ?, referred_by_doctor_id = ?, " +
                     "clinical_notes = ?, sample_collection_date = ?, report_submitted_date = ?, " +
                     "testing_by = ?, approved_by = ?, status = ?, file_path = ?, updated_at = ? " +
                     "WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, report.getPatientId());
            pstmt.setString(2, report.getReportDate());
            pstmt.setObject(3, report.getReferredByDoctorId());
            pstmt.setString(4, report.getClinicalNotes());
            pstmt.setString(5, report.getSampleCollectionDate());
            pstmt.setString(6, report.getReportSubmittedDate());
            pstmt.setString(7, report.getTestingBy());
            pstmt.setString(8, report.getApprovedBy());
            pstmt.setString(9, report.getStatus());
            pstmt.setString(10, report.getFilePath());
            pstmt.setLong(11, System.currentTimeMillis());
            pstmt.setInt(12, report.getId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Delete a report (soft delete)
     */
    public boolean deleteReport(int reportId) {
        String sql = "UPDATE reports SET status = 'Deleted', updated_at = ? WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis());
            pstmt.setInt(2, reportId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ============ REPORT TEST RESULT METHODS ============

    /**
     * Get test results for a report
     */
    public List<ReportTestResult> getReportTestResults(int reportId) {
        try (Connection conn = dbManager.getConnection()) {
            return getReportTestResults(conn, reportId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * Get test results for a report (with connection)
     */
    private List<ReportTestResult> getReportTestResults(Connection conn, int reportId) {
        List<ReportTestResult> testResults = new ArrayList<>();
        String sql = "SELECT * FROM report_test_results WHERE report_id = ? ORDER BY id ASC";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, reportId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    testResults.add(mapRowToReportTestResult(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return testResults;
    }

    /**
     * Add a test result to a report
     */
    public int addTestResult(ReportTestResult testResult) {
        String sql = "INSERT INTO report_test_results (report_id, test_id, sub_test_id, test_name, " +
                     "sub_test_name, result_value, unit, normal_range_min, normal_range_max, " +
                     "is_abnormal, notes, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, testResult.getReportId());
            pstmt.setInt(2, testResult.getTestId());
            pstmt.setObject(3, testResult.getSubTestId()); // Null-safe
            pstmt.setString(4, testResult.getTestName());
            pstmt.setString(5, testResult.getSubTestName());
            pstmt.setString(6, testResult.getResultValue());
            pstmt.setString(7, testResult.getUnit());
            pstmt.setDouble(8, testResult.getNormalRangeMin());
            pstmt.setDouble(9, testResult.getNormalRangeMax());
            pstmt.setBoolean(10, testResult.getIsAbnormal());
            pstmt.setString(11, testResult.getNotes());
            pstmt.setLong(12, testResult.getCreatedAt());
            pstmt.setLong(13, System.currentTimeMillis());
            
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
     * Update a test result
     */
    public boolean updateTestResult(ReportTestResult testResult) {
        String sql = "UPDATE report_test_results SET result_value = ?, is_abnormal = ?, " +
                     "notes = ?, updated_at = ? WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, testResult.getResultValue());
            pstmt.setBoolean(2, testResult.getIsAbnormal());
            pstmt.setString(3, testResult.getNotes());
            pstmt.setLong(4, System.currentTimeMillis());
            pstmt.setInt(5, testResult.getId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Delete a test result
     */
    public boolean deleteTestResult(int testResultId) {
        String sql = "DELETE FROM report_test_results WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, testResultId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ============ HELPER METHODS ============

    private Report mapRowToReport(ResultSet rs) throws SQLException {
        Report report = new Report();
        report.setId(rs.getInt("id"));
        report.setPatientId(rs.getInt("patient_id"));
        report.setReportDate(rs.getString("report_date"));
        
        Object doctorId = rs.getObject("referred_by_doctor_id");
        report.setReferredByDoctorId(doctorId != null ? rs.getInt("referred_by_doctor_id") : null);
        
        report.setClinicalNotes(rs.getString("clinical_notes"));
        report.setSampleCollectionDate(rs.getString("sample_collection_date"));
        report.setReportSubmittedDate(rs.getString("report_submitted_date"));
        report.setTestingBy(rs.getString("testing_by"));
        report.setApprovedBy(rs.getString("approved_by"));
        report.setStatus(rs.getString("status"));
        report.setFilePath(rs.getString("file_path"));
        report.setCreatedAt(rs.getLong("created_at"));
        report.setUpdatedAt(rs.getLong("updated_at"));
        return report;
    }

    private ReportTestResult mapRowToReportTestResult(ResultSet rs) throws SQLException {
        ReportTestResult testResult = new ReportTestResult();
        testResult.setId(rs.getInt("id"));
        testResult.setReportId(rs.getInt("report_id"));
        testResult.setTestId(rs.getInt("test_id"));
        
        Object subTestId = rs.getObject("sub_test_id");
        testResult.setSubTestId(subTestId != null ? rs.getInt("sub_test_id") : null);
        
        testResult.setTestName(rs.getString("test_name"));
        testResult.setSubTestName(rs.getString("sub_test_name"));
        testResult.setResultValue(rs.getString("result_value"));
        testResult.setUnit(rs.getString("unit"));
        testResult.setNormalRangeMin(rs.getDouble("normal_range_min"));
        testResult.setNormalRangeMax(rs.getDouble("normal_range_max"));
        testResult.setIsAbnormal(rs.getBoolean("is_abnormal"));
        testResult.setNotes(rs.getString("notes"));
        testResult.setCreatedAt(rs.getLong("created_at"));
        testResult.setUpdatedAt(rs.getLong("updated_at"));
        return testResult;
    }
}
