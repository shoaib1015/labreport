package labreport.service;

import labreport.model.LabProfile;
import labreport.db.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

public class LabProfileService {
    private DatabaseManager dbManager;

    public LabProfileService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Get the lab profile (usually only one exists)
     */
    public LabProfile getLabProfile() {
        String sql = "SELECT * FROM lab_profile WHERE id = 1 LIMIT 1";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return mapRowToLabProfile(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Create or update the lab profile
     */
    public boolean saveLabProfile(LabProfile profile) {
        LabProfile existing = getLabProfile();
        
        if (existing != null) {
            return updateLabProfile(profile);
        } else {
            return createLabProfile(profile);
        }
    }

    /**
     * Create a new lab profile
     */
    public boolean createLabProfile(LabProfile profile) {
        String sql = "INSERT INTO lab_profile (lab_name, address, city, state, postal_code, country, " +
                     "phones, email, website, operating_hours, owner_name, owner_phone, owner_email, " +
                     "franchise_type, report_header, report_footer, report_disclaimer, logo_data, " +
                     "gst_number, gst_rate, enable_gst, currency, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            setLabProfileParams(pstmt, profile);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Update an existing lab profile
     */
    public boolean updateLabProfile(LabProfile profile) {
        String sql = "UPDATE lab_profile SET lab_name = ?, address = ?, city = ?, state = ?, " +
                     "postal_code = ?, country = ?, phones = ?, email = ?, website = ?, " +
                     "operating_hours = ?, owner_name = ?, owner_phone = ?, owner_email = ?, " +
                     "franchise_type = ?, report_header = ?, report_footer = ?, report_disclaimer = ?, " +
                     "logo_data = ?, gst_number = ?, gst_rate = ?, enable_gst = ?, currency = ?, " +
                     "updated_at = ? WHERE id = 1";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, profile.getLabName());
            pstmt.setString(2, profile.getAddress());
            pstmt.setString(3, profile.getCity());
            pstmt.setString(4, profile.getState());
            pstmt.setString(5, profile.getPostalCode());
            pstmt.setString(6, profile.getCountry());
            pstmt.setString(7, profile.getPhones());
            pstmt.setString(8, profile.getEmail());
            pstmt.setString(9, profile.getWebsite());
            pstmt.setString(10, profile.getOperatingHours());
            pstmt.setString(11, profile.getOwnerName());
            pstmt.setString(12, profile.getOwnerPhone());
            pstmt.setString(13, profile.getOwnerEmail());
            pstmt.setString(14, profile.getFranchiseType());
            pstmt.setString(15, profile.getReportHeader());
            pstmt.setString(16, profile.getReportFooter());
            pstmt.setString(17, profile.getReportDisclaimer());
            pstmt.setBytes(18, profile.getLogoData());
            pstmt.setString(19, profile.getGstNumber());
            if (profile.getGstRate() != null) {
                pstmt.setDouble(20, profile.getGstRate());
            } else {
                pstmt.setNull(20, Types.REAL);
            }
            pstmt.setBoolean(21, profile.isEnableGst());
            pstmt.setString(22, profile.getCurrency());
            pstmt.setLong(23, System.currentTimeMillis());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Helper method to set parameters for lab profile prepared statements
     */
    private void setLabProfileParams(PreparedStatement pstmt, LabProfile profile) throws SQLException {
        pstmt.setString(1, profile.getLabName());
        pstmt.setString(2, profile.getAddress());
        pstmt.setString(3, profile.getCity());
        pstmt.setString(4, profile.getState());
        pstmt.setString(5, profile.getPostalCode());
        pstmt.setString(6, profile.getCountry());
        pstmt.setString(7, profile.getPhones());
        pstmt.setString(8, profile.getEmail());
        pstmt.setString(9, profile.getWebsite());
        pstmt.setString(10, profile.getOperatingHours());
        pstmt.setString(11, profile.getOwnerName());
        pstmt.setString(12, profile.getOwnerPhone());
        pstmt.setString(13, profile.getOwnerEmail());
        pstmt.setString(14, profile.getFranchiseType());
        pstmt.setString(15, profile.getReportHeader());
        pstmt.setString(16, profile.getReportFooter());
        pstmt.setString(17, profile.getReportDisclaimer());
        pstmt.setBytes(18, profile.getLogoData());
        pstmt.setString(19, profile.getGstNumber());
        if (profile.getGstRate() != null) {
            pstmt.setDouble(20, profile.getGstRate());
        } else {
            pstmt.setNull(20, Types.REAL);
        }
        pstmt.setBoolean(21, profile.isEnableGst());
        pstmt.setString(22, profile.getCurrency());
        pstmt.setLong(23, profile.getCreatedAt());
        pstmt.setLong(24, System.currentTimeMillis());
    }

    /**
     * Helper method to map a ResultSet row to a LabProfile object
     */
    private LabProfile mapRowToLabProfile(ResultSet rs) throws SQLException {
        LabProfile profile = new LabProfile();
        profile.setId(rs.getInt("id"));
        profile.setLabName(rs.getString("lab_name"));
        profile.setAddress(rs.getString("address"));
        profile.setCity(rs.getString("city"));
        profile.setState(rs.getString("state"));
        profile.setPostalCode(rs.getString("postal_code"));
        profile.setCountry(rs.getString("country"));
        profile.setPhones(rs.getString("phones"));
        profile.setEmail(rs.getString("email"));
        profile.setWebsite(rs.getString("website"));
        profile.setOperatingHours(rs.getString("operating_hours"));
        profile.setOwnerName(rs.getString("owner_name"));
        profile.setOwnerPhone(rs.getString("owner_phone"));
        profile.setOwnerEmail(rs.getString("owner_email"));
        profile.setFranchiseType(rs.getString("franchise_type"));
        profile.setReportHeader(rs.getString("report_header"));
        profile.setReportFooter(rs.getString("report_footer"));
        profile.setReportDisclaimer(rs.getString("report_disclaimer"));
        profile.setLogoData(rs.getBytes("logo_data"));
        profile.setGstNumber(rs.getString("gst_number"));
        Object gstRateObj = rs.getObject("gst_rate");
        profile.setGstRate(gstRateObj != null ? rs.getDouble("gst_rate") : null);
        profile.setEnableGst(rs.getBoolean("enable_gst"));
        profile.setCurrency(rs.getString("currency"));
        profile.setCreatedAt(rs.getLong("created_at"));
        profile.setUpdatedAt(rs.getLong("updated_at"));
        return profile;
    }
}
