package labreport.auth;

import labreport.db.DatabaseManager;
import labreport.logging.AppLogger;

import java.sql.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class PatientService {

    private static final Logger log = AppLogger.getLogger();
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^\\d{10}$");
    private static final DateTimeFormatter UTC_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    public static class ValidationResult {
        public boolean valid;
        public List<String> errors;

        public ValidationResult() {
            this.errors = new ArrayList<>();
            this.valid = true;
        }

        public void addError(String error) {
            this.valid = false;
            this.errors.add(error);
        }
    }

    public static class CreatePatientRequest {
        public String name;
        public String dob; // YYYY-MM-DD
        public String gender;
        public String contact_phone;
        public String contact_email;
        public String address;
        public Integer referring_doctor_id;
        public Integer created_by;
        public List<Integer> order_panels;
        public String priority;
        public String notes;
    }

    public static class CreatePatientResponse {
        public ValidationResult validationResult;
        public String sqlTransaction;
        public Map<String, Object> createdObjects;
    }

    public static CreatePatientResponse createPatient(CreatePatientRequest request) {
        CreatePatientResponse response = new CreatePatientResponse();
        response.validationResult = validateRequest(request);
        response.createdObjects = new HashMap<>();

        log.info("Creating patient: " + request.name);
        log.info("Validation result: " + response.validationResult.valid);

        if (!response.validationResult.valid) {
            log.warning("Validation errors: " + response.validationResult.errors);
            return response;
        }

        // Generate SQL transaction and created objects
        response.sqlTransaction = generateSQLTransaction(request);
        response.createdObjects = generateCreatedObjects(request);

        // Execute the transaction
        try {
            log.info("Executing database transaction for patient: " + request.name);
            executeTransaction(request);
            log.info("Patient and test orders successfully inserted into database");
        } catch (Exception e) {
            log.severe("Failed to create patient: " + e.getMessage());
            e.printStackTrace();
            response.validationResult.addError("Database error: " + e.getMessage());
            response.validationResult.valid = false;
        }

        return response;
    }

    private static ValidationResult validateRequest(CreatePatientRequest request) {
        ValidationResult result = new ValidationResult();

        // Required fields
        if (request.name == null || request.name.trim().isEmpty()) {
            result.addError("Name is required");
        }

        if (request.created_by == null) {
            result.addError("Created_by (user id) is required");
        }

        // Optional but validated fields
        if (request.dob != null && !request.dob.isEmpty()) {
            if (!isValidDate(request.dob)) {
                result.addError("DOB must be in YYYY-MM-DD format");
            }
        }

        if (request.gender != null && !request.gender.isEmpty()) {
            if (!isValidGender(request.gender)) {
                result.addError("Gender must be 'Male', 'Female', or 'Other'");
            }
        }

        if (request.contact_phone != null && !request.contact_phone.isEmpty()) {
            if (!isValidPhone(request.contact_phone)) {
                result.addError("Phone must be 10 digits");
            }
        }

        if (request.contact_email != null && !request.contact_email.isEmpty()) {
            if (!isValidEmail(request.contact_email)) {
                result.addError("Email format is invalid");
            }
        }

        if (request.priority != null && !request.priority.isEmpty()) {
            if (!isValidPriority(request.priority)) {
                result.addError("Priority must be 'Routine' or 'Urgent'");
            }
        }

        return result;
    }

    private static boolean isValidDate(String date) {
        return date.matches("^\\d{4}-\\d{2}-\\d{2}$");
    }

    private static boolean isValidGender(String gender) {
        return gender.equals("Male") || gender.equals("Female") || gender.equals("Other");
    }

    private static boolean isValidPhone(String phone) {
        return PHONE_PATTERN.matcher(phone).matches();
    }

    private static boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private static boolean isValidPriority(String priority) {
        return priority.equals("Routine") || priority.equals("Urgent");
    }

    private static String generateSQLTransaction(CreatePatientRequest request) {
        StringBuilder sql = new StringBuilder();
        String utcNow = ZonedDateTime.now(ZoneOffset.UTC).format(UTC_FORMATTER);

        sql.append("BEGIN;\n");
        sql.append("-- Insert patient record\n");
        sql.append("INSERT INTO patients (name, dob, gender, contact_phone, contact_email, address, referring_doctor_id, created_by, created_at)\n");
        sql.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);\n");
        sql.append("-- Placeholders: (1:name, 2:dob, 3:gender, 4:contact_phone, 5:contact_email, 6:address, 7:referring_doctor_id, 8:created_by, 9:created_at)\n");

        if (request.order_panels != null && !request.order_panels.isEmpty()) {
            sql.append("\n-- Insert test order for the created patient\n");
            sql.append("INSERT INTO test_order (patient_id, priority, notes, created_by, created_at)\n");
            sql.append("VALUES ((SELECT last_insert_rowid()), ?, ?, ?, ?);\n");
            sql.append("-- Placeholders: (10:priority, 11:notes, 12:created_by, 13:created_at)\n");

            sql.append("\n-- Insert test order panels\n");
            for (int i = 0; i < request.order_panels.size(); i++) {
                if (i > 0) {
                    sql.append(";\n");
                }
                sql.append("INSERT INTO test_order_panel (test_order_id, panel_id, created_at)\n");
                sql.append("VALUES ((SELECT last_insert_rowid()), ?, ?)");
                sql.append("\n-- Placeholders: (").append(14 + (i * 2)).append(":panel_id, ").append(15 + (i * 2)).append(":created_at)");
            }
            sql.append(";\n");
        }

        sql.append("COMMIT;");
        return sql.toString();
    }

    private static Map<String, Object> generateCreatedObjects(CreatePatientRequest request) {
        String utcNow = ZonedDateTime.now(ZoneOffset.UTC).format(UTC_FORMATTER);
        Map<String, Object> objects = new HashMap<>();

        // Patient object
        Map<String, Object> patient = new HashMap<>();
        patient.put("id", "[GENERATED_ID]");
        patient.put("name", request.name);
        patient.put("dob", request.dob);
        patient.put("gender", request.gender);
        patient.put("contact_phone", normalizePhone(request.contact_phone));
        patient.put("contact_email", request.contact_email);
        patient.put("address", request.address);
        patient.put("referring_doctor_id", request.referring_doctor_id);
        patient.put("created_by", request.created_by);
        patient.put("created_at", utcNow);

        objects.put("patient", patient);

        // Test orders
        if (request.order_panels != null && !request.order_panels.isEmpty()) {
            List<Map<String, Object>> testOrders = new ArrayList<>();
            Map<String, Object> testOrder = new HashMap<>();
            testOrder.put("id", "[GENERATED_ID]");
            testOrder.put("patient_id", "[FROM_PATIENT_ID]");
            testOrder.put("priority", request.priority != null ? request.priority : "Routine");
            testOrder.put("notes", request.notes);
            testOrder.put("created_by", request.created_by);
            testOrder.put("created_at", utcNow);

            List<Map<String, Object>> panels = new ArrayList<>();
            for (Integer panelId : request.order_panels) {
                Map<String, Object> panel = new HashMap<>();
                panel.put("panel_id", panelId);
                panel.put("created_at", utcNow);
                panels.add(panel);
            }
            testOrder.put("panels", panels);
            testOrders.add(testOrder);

            objects.put("testOrders", testOrders);
        }

        return objects;
    }

    private static String normalizePhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return null;
        }
        // Normalize to E.164 format: +91XXXXXXXXXX (assuming India)
        phone = phone.replaceAll("[^0-9]", "");
        if (phone.length() == 10) {
            return "+91" + phone;
        }
        return phone;
    }

    private static void executeTransaction(CreatePatientRequest request) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        String utcNow = ZonedDateTime.now(ZoneOffset.UTC).format(UTC_FORMATTER);
        int patientId = 0;
        int orderId = 0;

        log.info("===== START PATIENT CREATION TRANSACTION =====");
        log.info("Connection valid: " + (conn != null && !conn.isClosed()));
        
        try {
            log.info("Setting autocommit to false");
            conn.setAutoCommit(false);
            log.info("Autocommit disabled successfully");

            // Insert patient
            log.info("Preparing patient INSERT statement");
            String patientSql = "INSERT INTO patients (name, dob, gender, contact_phone, contact_email, address, referring_doctor_id, created_by, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(patientSql)) {
                log.info("Setting patient parameters:");
                log.info("  [1] name: " + request.name);
                pstmt.setString(1, request.name);
                
                log.info("  [2] dob: " + request.dob);
                pstmt.setString(2, request.dob);
                
                log.info("  [3] gender: " + request.gender);
                pstmt.setString(3, request.gender);
                
                String normalizedPhone = normalizePhone(request.contact_phone);
                log.info("  [4] contact_phone (normalized): " + normalizedPhone);
                pstmt.setString(4, normalizedPhone);
                
                log.info("  [5] contact_email: " + request.contact_email);
                pstmt.setString(5, request.contact_email);
                
                log.info("  [6] address: " + request.address);
                pstmt.setString(6, request.address);
                
                log.info("  [7] referring_doctor_id: " + request.referring_doctor_id);
                pstmt.setObject(7, request.referring_doctor_id);
                
                log.info("  [8] created_by: " + request.created_by);
                pstmt.setInt(8, request.created_by);
                
                log.info("  [9] created_at: " + utcNow);
                pstmt.setString(9, utcNow);
                
                log.info("Executing patient INSERT...");
                int patientRowsAffected = pstmt.executeUpdate();
                log.info("Patient INSERT rows affected: " + patientRowsAffected);
            }

            // Get the last inserted patient ID
            log.info("Retrieving last inserted patient ID");
            String getIdSql = "SELECT last_insert_rowid() as id";
            try (PreparedStatement stmt = conn.prepareStatement(getIdSql)) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    patientId = rs.getInt("id");
                    log.info("Retrieved patient ID: " + patientId);
                } else {
                    log.warning("Failed to retrieve patient ID from last_insert_rowid()");
                }
            }

            // Verify patient was inserted
            if (patientId <= 0) {
                log.warning("CRITICAL: Patient ID is invalid (patientId=" + patientId + "). Insertion may have failed.");
            } else {
                log.info("Patient insertion verified with ID: " + patientId);
            }

            // Insert test order if panels are provided
            if (request.order_panels != null && !request.order_panels.isEmpty()) {
                log.info("Panel list provided with " + request.order_panels.size() + " panels");
                
                if (patientId > 0) {
                    log.info("Preparing test order INSERT statement");
                    String orderSql = "INSERT INTO test_order (patient_id, priority, notes, created_by, created_at) " +
                            "VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement orderStmt = conn.prepareStatement(orderSql)) {
                        log.info("Setting test order parameters:");
                        log.info("  [1] patient_id: " + patientId);
                        orderStmt.setInt(1, patientId);
                        
                        String priority = request.priority != null ? request.priority : "Routine";
                        log.info("  [2] priority: " + priority);
                        orderStmt.setString(2, priority);
                        
                        log.info("  [3] notes: " + request.notes);
                        orderStmt.setString(3, request.notes);
                        
                        log.info("  [4] created_by: " + request.created_by);
                        orderStmt.setInt(4, request.created_by);
                        
                        log.info("  [5] created_at: " + utcNow);
                        orderStmt.setString(5, utcNow);
                        
                        log.info("Executing test order INSERT...");
                        int orderRowsAffected = orderStmt.executeUpdate();
                        log.info("Test order INSERT rows affected: " + orderRowsAffected);
                    }

                    // Get the last inserted test order ID
                    log.info("Retrieving last inserted test order ID");
                    try (PreparedStatement stmt = conn.prepareStatement(getIdSql)) {
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            orderId = rs.getInt("id");
                            log.info("Retrieved test order ID: " + orderId);
                        } else {
                            log.warning("Failed to retrieve test order ID from last_insert_rowid()");
                        }
                    }

                    // Insert test order panels
                    if (orderId > 0) {
                        log.info("Preparing panel batch insert for order ID: " + orderId);
                        Map<Integer, String> panelMap = new HashMap<>();

                        String panelSql = "INSERT INTO test_order_panel (test_order_id, panel_id, created_at, panel_name) VALUES (?, ?, ?, ?)";
                        log.info("Panel SQL: " + panelSql);
                        try (PreparedStatement panelStmt = conn.prepareStatement(panelSql)) {
                            // Build query to fetch all panel information for the provided panel IDs
                            StringBuilder panelQueryBuilder = new StringBuilder("SELECT panel_id, panel_name FROM Panels WHERE panel_id IN (");
                            log.info("panelQueryBuilder: " + panelQueryBuilder.toString());
                            log.info("panel IDs: " + request.order_panels.toString());
                            for (int i = 0; i < request.order_panels.size(); i++) {
                                if (i > 0) panelQueryBuilder.append(", ");
                                panelQueryBuilder.append("?");
                            }
                            panelQueryBuilder.append(")");
                            log.info(panelQueryBuilder.toString() + " with panel IDs: " + request.order_panels.toString());
                            
                            try (PreparedStatement panelFetchStmt = conn.prepareStatement(panelQueryBuilder.toString())) {
                                // Set all panel IDs in the WHERE IN clause
                                for (int i = 0; i < request.order_panels.size(); i++) {
                                    panelFetchStmt.setInt(i + 1, request.order_panels.get(i));
                                    log.info("Panel fetch query parameter [" + (i + 1) + "]: " + request.order_panels.get(i));
                                }
                                
                                ResultSet rs = panelFetchStmt.executeQuery();
                                
                                while (rs.next()) {
                                    panelMap.put(rs.getInt("panel_id"), rs.getString("panel_name"));
                                    log.info("Fetched panel: id=" + rs.getInt("panel_id") + ", name=" + rs.getString("panel_name"));
                                }
                            }

                            int panelCount = 0;
                            for (Integer panelId : request.order_panels) {
                                panelCount++;
                                log.info("Adding panel " + panelCount + ": panel_id=" + panelId);
                                panelStmt.setInt(1, orderId);
                                panelStmt.setInt(2, panelId);
                                panelStmt.setString(3, utcNow);
                                panelStmt.setString(4, panelMap.get(panelId));
                                panelStmt.addBatch();
                            }
                            
                            log.info("Executing batch insert for " + panelCount + " panels...");
                            int[] batchResults = panelStmt.executeBatch();
                            log.info("Batch insert completed. Results length: " + batchResults.length);
                            for (int i = 0; i < batchResults.length; i++) {
                                log.info("  Batch[" + i + "]: " + batchResults[i]);
                            }
                        }
                    } else {
                        log.warning("Test order ID is invalid (orderId=" + orderId + "). Panel insertion skipped.");
                    }
                } else {
                    log.warning("Patient ID is invalid (patientId=" + patientId + "). Test order insertion skipped.");
                }
            } else {
                log.info("No panels provided. Test order creation skipped.");
            }

            log.info("Committing transaction...");
            conn.commit();
            log.info("Transaction committed successfully");
            log.info("===== PATIENT CREATION TRANSACTION COMPLETED SUCCESSFULLY =====");
        } catch (SQLException e) {
            log.severe("SQL Exception during transaction: " + e.getMessage());
            log.severe("SQL State: " + e.getSQLState());
            log.severe("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            
            log.info("Rolling back transaction...");
            try {
                conn.rollback();
                log.info("Rollback completed");
            } catch (SQLException rollbackEx) {
                log.severe("Error during rollback: " + rollbackEx.getMessage());
            }
            
            throw e;
        } finally {
            log.info("Setting autocommit back to true");
            try {
                conn.setAutoCommit(true);
                log.info("Autocommit re-enabled");
            } catch (SQLException ex) {
                log.severe("Error resetting autocommit: " + ex.getMessage());
            }
            log.info("===== END PATIENT CREATION TRANSACTION =====");
        }
    }

    public static Map<String, String> getPatientById(int id) {
        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM patients WHERE id = ?");
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Map<String, String> patient = new HashMap<>();
                patient.put("id", String.valueOf(rs.getInt("id")));
                patient.put("name", rs.getString("name"));
                patient.put("dob", rs.getString("dob"));
                patient.put("gender", rs.getString("gender"));
                patient.put("contact_phone", rs.getString("contact_phone"));
                patient.put("contact_email", rs.getString("contact_email"));
                patient.put("address", rs.getString("address"));
                patient.put("referring_doctor_id", String.valueOf(rs.getInt("referring_doctor_id")));
                patient.put("created_by", String.valueOf(rs.getInt("created_by")));
                patient.put("created_at", rs.getString("created_at"));
                return patient;
            }
        } catch (SQLException e) {
            log.severe("Failed to fetch patient: " + e.getMessage());
        }
        return new HashMap<>();
    }
}
