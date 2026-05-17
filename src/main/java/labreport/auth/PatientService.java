package labreport.auth;

import labreport.auth.TestOrderComponentService;
import labreport.db.DatabaseManager;
import labreport.logging.AppLogger;

import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class PatientService {

    private static final Logger log = AppLogger.getLogger();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10}$");
    private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);

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

    public static CreatePatientResponse updatePatient(int patientId, CreatePatientRequest request) {
        CreatePatientResponse response = new CreatePatientResponse();
        response.validationResult = validateRequest(request);
        response.createdObjects = new HashMap<>();

        log.info("Updating patient: " + patientId + " - " + request.name);
        log.info("Validation result: " + response.validationResult.valid);

        if (!response.validationResult.valid) {
            log.warning("Validation errors: " + response.validationResult.errors);
            return response;
        }

        // Execute the update
        try {
            log.info("Executing database update for patient: " + patientId);
            executeUpdate(patientId, request);
            log.info("Patient successfully updated in database");

            // Return updated patient data
            Map<String, Object> patient = new HashMap<>();
            patient.put("id", patientId);
            patient.put("name", request.name);
            patient.put("dob", request.dob);
            patient.put("gender", request.gender);
            patient.put("contact_phone", request.contact_phone);
            patient.put("contact_email", request.contact_email);
            patient.put("address", request.address);
            patient.put("referring_doctor_id", request.referring_doctor_id);
            patient.put("created_by", request.created_by);
            response.createdObjects.put("patient", patient);
        } catch (Exception e) {
            log.severe("Failed to update patient: " + e.getMessage());
            e.printStackTrace();
            response.validationResult.addError("Database error: " + e.getMessage());
            response.validationResult.valid = false;
        }

        return response;
    }

    private static void executeUpdate(int patientId, CreatePatientRequest request) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        String utcNow = ZonedDateTime.now(ZoneOffset.UTC).format(UTC_FORMATTER);

        log.info("===== START PATIENT UPDATE TRANSACTION =====");

        try {
            conn.setAutoCommit(false);
            log.info("Autocommit disabled successfully");

            // Update patient record
            log.info("Preparing patient UPDATE statement");
            String patientSql = "UPDATE patients SET name = ?, dob = ?, gender = ?, contact_phone = ?, contact_email = ?, address = ?, referring_doctor_id = ?, updated_at = ? "
                    +
                    "WHERE id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(patientSql)) {
                log.info("Setting patient update parameters:");
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

                log.info("  [8] updated_at: " + utcNow);
                pstmt.setString(8, utcNow);

                log.info("  [9] patient id: " + patientId);
                pstmt.setInt(9, patientId);

                log.info("Executing patient UPDATE...");
                int rowsAffected = pstmt.executeUpdate();
                log.info("Patient UPDATE rows affected: " + rowsAffected);
            }

            log.info("Committing transaction...");
            conn.commit();
            log.info("Transaction committed successfully");
            log.info("===== PATIENT UPDATE TRANSACTION COMPLETED SUCCESSFULLY =====");
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
            log.info("===== END PATIENT UPDATE TRANSACTION =====");
        }
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

    /**
     * Calculate age group from date of birth (YYYY-MM-DD)
     * Returns 'Child' if age < 18, 'Adult' otherwise
     */
    private static String calculateAgeGroup(String dob) {
        try {
            if (dob == null || dob.isEmpty()) {
                return "Adult"; // Default to Adult if no DOB
            }

            LocalDate birthDate = LocalDate.parse(dob);
            LocalDate today = LocalDate.now();
            int age = (int) ChronoUnit.YEARS.between(birthDate, today);

            return age < 18 ? "Child" : "Adult";
        } catch (Exception e) {
            log.warning("Failed to calculate age group from DOB '" + dob + "': " + e.getMessage());
            return "Adult"; // Default to Adult if parsing fails
        }
    }

    private static String generateSQLTransaction(CreatePatientRequest request) {
        StringBuilder sql = new StringBuilder();
        String utcNow = ZonedDateTime.now(ZoneOffset.UTC).format(UTC_FORMATTER);

        sql.append("BEGIN;\n");
        sql.append("-- Insert patient record\n");
        sql.append(
                "INSERT INTO patients (name, dob, gender, contact_phone, contact_email, address, referring_doctor_id, created_by, created_at)\n");
        sql.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);\n");
        sql.append(
                "-- Placeholders: (1:name, 2:dob, 3:gender, 4:contact_phone, 5:contact_email, 6:address, 7:referring_doctor_id, 8:created_by, 9:created_at)\n");

        if (request.order_panels != null && !request.order_panels.isEmpty()) {
            sql.append("\n-- Insert test order for the created patient\n");
            sql.append(
                    "INSERT INTO test_order (patient_id, priority, notes, created_by, created_at, panel_id, panel_name)\n");
            sql.append("VALUES ((SELECT last_insert_rowid()), ?, ?, ?, ?, ?, ?);\n");
            sql.append(
                    "-- Placeholders: (10:priority, 11:notes, 12:created_by, 13:created_at, 14:panel_id, 15:panel_name)\n");
        }
        log.info(sql.toString());
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
            String patientSql = "INSERT INTO patients (name, dob, gender, contact_phone, contact_email, address, referring_doctor_id, created_by, created_at) "
                    +
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
                log.warning(
                        "CRITICAL: Patient ID is invalid (patientId=" + patientId + "). Insertion may have failed.");
            } else {
                log.info("Patient insertion verified with ID: " + patientId);
            }

            // Insert test order if panels are provided
            if (request.order_panels != null && !request.order_panels.isEmpty()) {
                log.info("Panel list provided with " + request.order_panels.size() + " panels");

                if (patientId > 0) {
                    // Fetch panel information for all provided panel IDs
                    Map<Integer, String> panelMap = new HashMap<>();

                    log.info("Fetching panel information for panel IDs: " + request.order_panels.toString());
                    StringBuilder panelQueryBuilder = new StringBuilder(
                            "SELECT panel_id, panel_name FROM panels WHERE panel_id IN (");
                    for (int i = 0; i < request.order_panels.size(); i++) {
                        if (i > 0)
                            panelQueryBuilder.append(", ");
                        panelQueryBuilder.append("?");
                    }
                    panelQueryBuilder.append(")");

                    try (PreparedStatement panelFetchStmt = conn.prepareStatement(panelQueryBuilder.toString())) {
                        // Set all panel IDs in the WHERE IN clause
                        for (int i = 0; i < request.order_panels.size(); i++) {
                            panelFetchStmt.setInt(i + 1, request.order_panels.get(i));
                            log.info("Panel fetch query parameter [" + (i + 1) + "]: " + request.order_panels.get(i));
                        }

                        ResultSet rs = panelFetchStmt.executeQuery();
                        while (rs.next()) {
                            panelMap.put(rs.getInt("panel_id"), rs.getString("panel_name"));
                            log.info("Fetched panel: id=" + rs.getInt("panel_id") + ", name="
                                    + rs.getString("panel_name"));
                        }
                    }

                    // Create ONE test_order for EACH selected panel and INSERT COMPONENTS
                    log.info("Preparing test order INSERT statement - will create " + request.order_panels.size()
                            + " orders (one per panel)");
                    String orderSql = "INSERT INTO test_order (patient_id, priority, notes, created_by, created_at, panel_id, panel_name) "
                            +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)";

                    try (PreparedStatement orderStmt = conn.prepareStatement(orderSql)) {
                        String priority = request.priority != null ? request.priority : "Routine";

                        // Calculate age group from DOB
                        String ageGroup = calculateAgeGroup(request.dob);
                        String gender = request.gender != null ? request.gender : "Other";

                        int panelCount = 0;
                        for (Integer panelId : request.order_panels) {
                            panelCount++;
                            log.info("Creating test order " + panelCount + " for panel_id=" + panelId);

                            log.info("Setting test order parameters:");
                            log.info("  [1] patient_id: " + patientId);
                            orderStmt.setInt(1, patientId);

                            log.info("  [2] priority: " + priority);
                            orderStmt.setString(2, priority);

                            log.info("  [3] notes: " + request.notes);
                            orderStmt.setString(3, request.notes);

                            log.info("  [4] created_by: " + request.created_by);
                            orderStmt.setInt(4, request.created_by);

                            log.info("  [5] created_at: " + utcNow);
                            orderStmt.setString(5, utcNow);

                            log.info("  [6] panel_id: " + panelId);
                            orderStmt.setInt(6, panelId);

                            String panelName = panelMap.get(panelId);
                            log.info("  [7] panel_name: " + panelName);
                            orderStmt.setString(7, panelName);

                            // Execute individual insert
                            int rowsInserted = orderStmt.executeUpdate();
                            log.info("Test order insert result: " + rowsInserted);

                            // Get the generated test_order ID using SQLite's last_insert_rowid()
                            int testOrderId = 0;
                            try (PreparedStatement getIdStmt = conn
                                    .prepareStatement("SELECT last_insert_rowid() as id")) {
                                ResultSet rs = getIdStmt.executeQuery();
                                if (rs.next()) {
                                    testOrderId = rs.getInt("id");
                                    log.info("Generated test_order ID: " + testOrderId);
                                }
                            }

                            // NOW INSERT COMPONENTS for this test order
                            if (testOrderId > 0) {
                                try {
                                    log.info("Inserting components for test_order_id=" + testOrderId +
                                            " (panel=" + panelName + ", ageGroup=" + ageGroup + ", gender=" + gender
                                            + ")");

                                    int componentsInserted = TestOrderComponentService.insertTestOrderComponents(
                                            testOrderId,
                                            patientId,
                                            panelName,
                                            panelId,
                                            ageGroup,
                                            gender);

                                    log.info("Successfully inserted " + componentsInserted
                                            + " components for test_order_id=" + testOrderId);
                                } catch (Exception componentEx) {
                                    log.severe("Failed to insert components for test_order_id=" + testOrderId + ": "
                                            + componentEx.getMessage());
                                    log.warning("Continuing with next test order despite component insertion failure");
                                    // Continue with next panel instead of failing
                                }
                            } else {
                                log.warning("Could not retrieve generated test_order ID");
                            }
                        }

                        log.info("Completed insertion of " + panelCount + " test orders with components");
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
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT p.id, p.name, p.dob, p.gender, p.contact_phone, p.contact_email, p.address, p.referring_doctor_id, p.created_by, p.created_at, "
                            +
                            "rd.full_name AS referring_doctor_name, " +
                            "(SELECT COUNT(*) FROM test_order WHERE patient_id = p.id and status IN ('Ordered', 'Sample Collected', 'In Lab','Pending')) AS active_tests, "
                            +
                            "(SELECT COUNT(*) FROM test_order WHERE patient_id = p.id and status IN ('Ordered','Sample Collected','Pending','InLab')) AS pending_results "
                            +
                            "FROM patients p " +
                            "LEFT JOIN referring_doctors rd ON p.referring_doctor_id = rd.doctor_id " +
                            "WHERE p.id = ?");
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
                // Joined field from ReferringDoctors
                String referringDoctorName = rs.getString("referring_doctor_name");
                patient.put("referring_doctor_name", referringDoctorName != null ? referringDoctorName : "-");

                // Aggregated counts from test_order
                patient.put("active_tests", String.valueOf(rs.getInt("active_tests")));
                patient.put("pending_results", String.valueOf(rs.getInt("pending_results")));
                return patient;
            }
        } catch (SQLException e) {
            log.severe("Failed to fetch patient: " + e.getMessage());
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    public static String getDashboardStatsJson() throws SQLException {
        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT " +
                    "  (SELECT COUNT(*) FROM patients) AS total_patients, " +
                    "  (SELECT COUNT(*) FROM test_order WHERE status IN ('Ordered','SampleCollected','InLab')) AS active_tests, "
                    +
                    "  (SELECT COUNT(*) FROM test_order WHERE status IN ('Ordered','SampleCollected','InLab')) AS pending_results, "
                    +
                    "  (SELECT COUNT(*) FROM test_order WHERE status IN ('ReportReady','Delivered')) AS completed_reports;");
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                // Shouldn't happen; return zeros as fallback
                return "{\"totalPatients\":0,\"activeTests\":0,\"pendingResults\":0,\"completedReports\":0}";
            }

            long totalPatients = rs.getLong("total_patients");
            long activeTests = rs.getLong("active_tests");
            long pendingResults = rs.getLong("pending_results");
            long completedReports = rs.getLong("completed_reports");

            // Build JSON (simple, safe for numeric values)
            String json = String.format(
                    "{\"totalPatients\":%d,\"activeTests\":%d,\"pendingResults\":%d,\"completedReports\":%d}",
                    totalPatients, activeTests, pendingResults, completedReports);

            return json;
        } catch (SQLException e) {
            log.severe("Failed to fetch patient: " + e.getMessage());
        }
        return "{\"totalPatients\":0,\"activeTests\":0,\"pendingResults\":0,\"completedReports\":0}";
    }

    public static String getAllPatientsJson() throws SQLException {
        StringBuilder json = new StringBuilder();
        json.append("{\"patients\":[");

        String sql = "SELECT p.id, p.name, p.dob, p.gender, p.created_at, p.referring_doctor_id, d.full_name AS referring_doctor_name FROM patients p LEFT JOIN referring_doctors d ON p.referring_doctor_id = d.doctor_id ORDER BY p.created_at DESC";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            boolean first = true;
            while (rs.next()) {
                if (!first) {
                    json.append(",");
                }
                first = false;

                int id = rs.getInt("id");
                String name = rs.getString("name");
                String dob = rs.getString("dob");
                String gender = rs.getString("gender");
                String createdAt = rs.getString("created_at");
                int referringDoctorId = rs.getInt("referring_doctor_id");
                String referringDoctorName = rs.getString("referring_doctor_name");
                log.info("referringDoctorName"+referringDoctorName);
                json.append("{\"id\":").append(id)
                        .append(",\"name\":\"").append(escapeJson(name)).append("\"")
                        .append(",\"dob\":").append(dob != null ? ("\"" + escapeJson(dob) + "\"") : "null")
                        .append(",\"gender\":").append(gender != null ? ("\"" + escapeJson(gender) + "\"") : "null")
                        .append(",\"created_at\":")
                        .append(createdAt != null ? ("\"" + escapeJson(createdAt) + "\"") : "null")
                        .append(",\"referring_doctor_id\":").append(referringDoctorId)
                        .append(",\"referring_doctor_name\":").append(referringDoctorName != null ? ("\"" + escapeJson(referringDoctorName) + "\"") : "null")
                        .append("}");
            }

            json.append("]}");
            return json.toString();
        } catch (SQLException e) {
            log.severe("Failed to fetch all patients: " + e.getMessage());
            return "{\"patients\":[]}";
        }
    }

    public static String getRecentPatientsJson() throws SQLException {
        return getRecentPatientsJson(5, "created_at", "DESC");
    }

    public static String getRecentPatientsJson(int limit, String sort, String order) throws SQLException {
        StringBuilder json = new StringBuilder();
        json.append("{\"patients\":[");

        String safeSort = validateSortColumn(sort);
        String safeOrder = validateOrder(order);

        String sql = "SELECT id, name, dob, gender, created_at FROM patients " +
                "ORDER BY " + safeSort + " " + safeOrder + " LIMIT ?";

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            boolean first = true;
            while (rs.next()) {
                if (!first) {
                    json.append(",");
                }
                first = false;

                int id = rs.getInt("id");
                String name = rs.getString("name");
                String dob = rs.getString("dob");
                String gender = rs.getString("gender");
                String createdAt = rs.getString("created_at");

                json.append("{\"id\":").append(id)
                        .append(",\"name\":\"").append(escapeJson(name)).append("\"")
                        .append(",\"dob\":").append(dob != null ? ("\"" + escapeJson(dob) + "\"") : "null")
                        .append(",\"gender\":").append(gender != null ? ("\"" + escapeJson(gender) + "\"") : "null")
                        .append(",\"created_at\":")
                        .append(createdAt != null ? ("\"" + escapeJson(createdAt) + "\"") : "null")
                        .append("}");
            }

            json.append("]}");
            return json.toString();
        } catch (SQLException e) {
            log.severe("Failed to fetch recent patients: " + e.getMessage());
            return "{\"patients\":[]}";
        }
    }

    public static String searchPatientsJson(String search, String gender, String createdAt) throws SQLException {
        StringBuilder json = new StringBuilder();
        json.append("{\"patients\":[");

        StringBuilder sql = new StringBuilder("SELECT id, name, dob, gender, created_at FROM patients");
        java.util.List<String> filters = new java.util.ArrayList<>();
        if (search != null && !search.isEmpty()) {
            if (search.matches("\\d+")) {
                filters.add("(id = ? OR LOWER(name) LIKE ?)");
            } else {
                filters.add("LOWER(name) LIKE ?");
            }
        }
        if (gender != null && !gender.isEmpty()) {
            filters.add("gender = ?");
        }
        if (createdAt != null && !createdAt.isEmpty()) {
            filters.add("created_at LIKE ?");
        }

        if (!filters.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", filters));
        }

        sql.append(" ORDER BY created_at DESC");

        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql.toString());

            int index = 1;
            if (search != null && !search.isEmpty()) {
                if (search.matches("\\d+")) {
                    stmt.setInt(index++, Integer.parseInt(search));
                    stmt.setString(index++, "%" + search.toLowerCase() + "%");
                } else {
                    stmt.setString(index++, "%" + search.toLowerCase() + "%");
                }
            }
            if (gender != null && !gender.isEmpty()) {
                stmt.setString(index++, gender);
            }
            if (createdAt != null && !createdAt.isEmpty()) {
                stmt.setString(index++, createdAt.length() == 10 ? createdAt + "%" : createdAt + "%");
            }

            ResultSet rs = stmt.executeQuery();
            boolean first = true;
            while (rs.next()) {
                if (!first) {
                    json.append(",");
                }
                first = false;

                int id = rs.getInt("id");
                String name = rs.getString("name");
                String dob = rs.getString("dob");
                String genderValue = rs.getString("gender");
                String createdAtValue = rs.getString("created_at");

                json.append("{\"id\":").append(id)
                        .append(",\"name\":\"").append(escapeJson(name)).append("\"")
                        .append(",\"dob\":").append(dob != null ? ("\"" + escapeJson(dob) + "\"") : "null")
                        .append(",\"gender\":")
                        .append(genderValue != null ? ("\"" + escapeJson(genderValue) + "\"") : "null")
                        .append(",\"created_at\":")
                        .append(createdAtValue != null ? ("\"" + escapeJson(createdAtValue) + "\"") : "null")
                        .append("}");
            }

            json.append("]}");
            return json.toString();
        } catch (SQLException e) {
            log.severe("Failed to search patients: " + e.getMessage());
            return "{\"patients\":[]}";
        }
    }

    public static void deletePatient(int patientId) throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        String sql = "DELETE FROM patients WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            int rows = stmt.executeUpdate();
            log.info("Deleted patient id=" + patientId + ", rows affected: " + rows);
        }
    }

    private static String validateSortColumn(String sort) {
        if (sort == null) {
            return "created_at";
        }
        switch (sort) {
            case "id":
            case "name":
            case "dob":
            case "gender":
            case "created_at":
                return sort;
            default:
                return "created_at";
        }
    }

    public static String getPatientTestOrdersJson(int patientId) throws SQLException {
        StringBuilder json = new StringBuilder();
        json.append("{\"testOrders\":[");

        String sql = "SELECT t.id as order_id, t.panel_name, t.sample_collected_at, t.notes, t.status, t.created_at, " +
                "c.id as component_row_id, c.component_name, c.result_value, c.unit, c.reference_range, c.flag " +
                "FROM test_order t " +
                "JOIN test_order_component c ON t.id = c.test_order_id " +
                "WHERE t.patient_id = ? " +
                "ORDER BY t.id, c.id";
        log.info("Executing SQL to fetch test orders for patient_id=" + patientId + ": " + sql);
        try {
            Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, patientId);
            ResultSet rs = stmt.executeQuery();
            log.info(rs.toString());
            boolean first = true;
            while (rs.next()) {
                if (!first) {
                    json.append(",");
                }
                first = false;
                int id = rs.getInt("order_id");
                String panelName = rs.getString("panel_name");
                String componentName = rs.getString("component_name");
                String referenceRange = rs.getString("reference_range");
                String resultValue = rs.getString("result_value");
                String unit = rs.getString("unit");
                String flag = rs.getString("flag");
                String status = rs.getString("status");
                String createdAt = rs.getString("created_at");
                String sampleCollectedAt = rs.getString("sample_collected_at");
                String notes = rs.getString("notes");
                int componentRowId = rs.getInt("component_row_id");

                json.append("{\"id\":").append(id)
                        .append(",\"componentRowId\":").append(componentRowId)
                        .append(",\"panelName\":\"").append(escapeJson(panelName)).append("\"")
                        .append(",\"componentName\":\"").append(escapeJson(componentName)).append("\"")
                        .append(",\"unit\":").append(unit != null ? ("\"" + escapeJson(unit) + "\"") : "\"\"")
                        .append(",\"referenceRange\":\"").append(escapeJson(referenceRange)).append("\"")
                        .append(",\"resultValue\":")
                        .append(resultValue != null ? ("\"" + escapeJson(resultValue) + "\"") : "\"\"")
                        .append(",\"flag\":").append(flag != null ? ("\"" + escapeJson(flag) + "\"") : "\"Normal\"")
                        .append(",\"status\":")
                        .append(status != null ? ("\"" + escapeJson(status) + "\"") : "\"Pending\"")
                        .append(",\"created_at\":")
                        .append(createdAt != null ? ("\"" + escapeJson(createdAt) + "\"") : "null")
                        .append(",\"sampleCollectedAt\":")
                        .append(sampleCollectedAt != null ? ("\"" + escapeJson(sampleCollectedAt) + "\"") : "null")
                        .append(",\"notes\":").append(notes != null ? ("\"" + escapeJson(notes) + "\"") : "\"\"")
                        .append("}");
            }

            json.append("]}");
            return json.toString();
        } catch (SQLException e) {
            log.severe("Failed to fetch test orders: " + e.getMessage());
            return "{\"testOrders\":[]}";
        }
    }

    /**
     * Update test_order record with sample collection date, status, notes.
     */
    public static void updateTestOrder(int testOrderId, String sampleCollectedAt, String status, String notes)
            throws SQLException {
        Connection conn = DatabaseManager.getConnection();
        String utcNow = ZonedDateTime.now(ZoneOffset.UTC).format(UTC_FORMATTER);

        String sql = "UPDATE test_order SET sample_collected_at = ?, status = ?, notes = ?, updated_at = ? WHERE id = ?";
        log.info("Updating test_order id=" + testOrderId + " sampleCollectedAt=" + sampleCollectedAt + " status="
                + status);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sampleCollectedAt);
            stmt.setString(2, status);
            stmt.setString(3, notes);
            stmt.setString(4, utcNow);
            stmt.setInt(5, testOrderId);
            int rows = stmt.executeUpdate();
            log.info("test_order updated, rows affected: " + rows);
        }
    }

    private static String validateOrder(String order) {
        if (order == null) {
            return "DESC";
        }
        String normalized = order.trim().toUpperCase();
        if ("ASC".equals(normalized) || "DESC".equals(normalized)) {
            return normalized;
        }
        return "DESC";
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

}
