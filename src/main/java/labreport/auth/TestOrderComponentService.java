package labreport.auth;

import labreport.db.DatabaseManager;
import labreport.logging.AppLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Service for managing test order components.
 * Generates SQL INSERT statements and inserts components for test orders
 * based on panel name, age group, and gender.
 */
public class TestOrderComponentService {

    private static final Logger log = AppLogger.getLogger();

    /**
     * Generates INSERT statements and JSON with SQL transaction for test_order_component.
     * Calculates patient age from DOB and applies gender/age prioritization rules.
     *
     * @param testOrderId  The test order ID
     * @param patientId    The patient ID
     * @param panelName    The panel name ('CBC', 'Lipid Profile', etc.)
     * @param panelId      The panel ID
     * @param patientDob   The patient DOB as string (YYYY-MM-DD format)
     * @param gender       The gender ('Male', 'Female', or 'Other')
     * @return JSON object with sqlTransaction and insertedComponents
     */
    public static String generateInsertStatementsWithAge(
        int testOrderId,
        int patientId,
        String panelName,
        int panelId,
        String patientDob,
        String gender) {

        try {
            // Calculate patient age
            LocalDate dob = LocalDate.parse(patientDob);
            int patientAge = Period.between(dob, LocalDate.now()).getYears();
            log.info("Calculated patient age: " + patientAge + " from DOB: " + patientDob);

            // Get matching components with prioritization
            List<Map<String, Object>> components = getComponentsByAgeAndGender(
                    panelId, patientAge, gender);

            if (components.isEmpty()) {
                log.warning("No components found for panel_id: " + panelId +
                    ", age: " + patientAge + ", gender: " + gender);
            }

            // Build transaction and JSON response
            StringBuilder sqlTransaction = new StringBuilder("BEGIN;\n");
            List<String> insertedComponents = new ArrayList<>();

            for (Map<String, Object> component : components) {
                String insertStatement = buildInsertStatementWithAge(testOrderId, component);
                sqlTransaction.append(insertStatement).append(";\n");

                // Build JSON component entry manually
                Map<String, Object> jsonComponent = new LinkedHashMap<>();
                jsonComponent.put("component_id", component.get("component_id"));
                jsonComponent.put("component_name", component.get("component_name"));
                jsonComponent.put("unit", component.get("unit"));
                jsonComponent.put("reference_range", component.get("normal_range"));
                jsonComponent.put("age_range", component.get("ageRange"));
                jsonComponent.put("gender", component.get("gender"));

                insertedComponents.add(toJsonObject(jsonComponent));
            }

            sqlTransaction.append("COMMIT;");

            // Final JSON result string
            Map<String, Object> resultMap = new LinkedHashMap<>();
            resultMap.put("sqlTransaction", sqlTransaction.toString());
            resultMap.put("insertedComponents", insertedComponents);

            String resultJson = toJsonObject(resultMap);

            log.info("Generated SQL transaction with " + insertedComponents.size() +
                " components for test_order_id: " + testOrderId);

            return resultJson;

        } catch (Exception e) {
            log.severe("Failed to generate INSERT statements: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }


    /**
     * Generates INSERT statements for test_order_component table.
     * Queries components table based on panel_name, age_group, and gender.
     *
     * @param testOrderId   The test order ID
     * @param patientId     The patient ID
     * @param panelName     The panel name ('CBC', 'Lipid Profile', 'Liver Function Test')
     * @param panelId       The panel ID
     * @param ageGroup      The age group ('Child' or 'Adult')
     * @param gender        The gender ('Male', 'Female', or 'Other')
     * @return List of SQL INSERT statements as strings
     */
    public static List<String> generateInsertStatements(
            int testOrderId,
            int patientId,
            String panelName,
            int panelId,
            String ageGroup,
            String gender) {

        List<String> insertStatements = new ArrayList<>();

        try {
            List<Map<String, Object>> components = getComponentsByPanelAndDemographics(
                    panelName, ageGroup, gender);

            for (Map<String, Object> component : components) {
                String insertStatement = buildInsertStatement(
                        testOrderId,
                        component);
                insertStatements.add(insertStatement);
            }

            log.info("Generated " + insertStatements.size() + " INSERT statements for test_order_id: " + testOrderId);

        } catch (Exception e) {
            log.severe("Failed to generate INSERT statements: " + e.getMessage());
            throw new RuntimeException(e);
        }

        return insertStatements;
    }

    /**
     * Inserts test order components into the database.
     * Automatically generates INSERT statements and executes them.
     *
     * @param testOrderId   The test order ID
     * @param patientId     The patient ID
     * @param panelName     The panel name
     * @param panelId       The panel ID
     * @param ageGroup      The age group
     * @param gender        The gender
     * @return Number of rows inserted
     */
    public static int insertTestOrderComponents(
            int testOrderId,
            int patientId,
            String panelName,
            int panelId,
            String ageGroup,
            String gender) {

        int totalRowsInserted = 0;

        try {
            List<Map<String, Object>> components = getComponentsByPanelAndDemographics(
                    panelName, ageGroup, gender);

            Connection conn = DatabaseManager.getConnection();

            String insertSql = "INSERT INTO test_order_component " +
                    "(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, NULL, 'Normal', datetime('now'))";

            for (Map<String, Object> component : components) {
                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    stmt.setInt(1, testOrderId);
                    stmt.setInt(2, (Integer) component.get("component_id"));
                    stmt.setString(3, (String) component.get("component_name"));
                    stmt.setString(4, (String) component.get("unit"));
                    stmt.setString(5, (String) component.get("normal_range"));

                    int rowsAffected = stmt.executeUpdate();
                    totalRowsInserted += rowsAffected;

                    log.info("Inserted component: " + component.get("component_name") +
                            " for test_order_id: " + testOrderId);
                }
            }

            log.info("Total rows inserted for test_order_id " + testOrderId + ": " + totalRowsInserted);

        } catch (Exception e) {
            log.severe("Failed to insert test order components: " + e.getMessage());
            throw new RuntimeException(e);
        }

        return totalRowsInserted;
    }

    /**
     * Retrieves components by age and gender with prioritization rules.
     * Priority: Exact gender match > 'All' gender > fallback to '1-100' age range
     *
     * @param panelId    The panel ID
     * @param patientAge The patient age in years
     * @param gender     The gender ('Male', 'Female', or 'Other')
     * @return List of prioritized component records
     */
    private static List<Map<String, Object>> getComponentsByAgeAndGender(
            int panelId,
            int patientAge,
            String gender) throws Exception {

        List<Map<String, Object>> components = new ArrayList<>();

        try {
            Connection conn = DatabaseManager.getConnection();

            // First, try to get components with exact age range and gender
            String sql = "SELECT component_id, component_name, unit, normal_range, remarks, ageRange, gender " +
                    "FROM components " +
                    "WHERE panel_id = ? " +
                    "AND status = 'Active' " +
                    "AND ageRange != '1-100'";

            List<Map<String, Object>> exactMatches = new ArrayList<>();
            List<Map<String, Object>> genderSpecificMatches = new ArrayList<>();
            List<Map<String, Object>> genderAllMatches = new ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, panelId);

                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    String ageRange = rs.getString("ageRange");
                    String componentGender = rs.getString("gender");

                    // Check if age is within range
                    if (isAgeInRange(patientAge, ageRange)) {
                        Map<String, Object> component = buildComponentMap(rs);

                        // Apply gender prioritization
                        if (!"Other".equalsIgnoreCase(gender) && componentGender.equalsIgnoreCase(gender)) {
                            genderSpecificMatches.add(component);
                        } else if ("All".equalsIgnoreCase(componentGender)) {
                            genderAllMatches.add(component);
                        }
                    }
                }
            }

            // Combine with priority: gender-specific > all > fallback
            components.addAll(genderSpecificMatches);
            components.addAll(genderAllMatches);

            // If no matches found, fall back to '1-100' (All Ages)
            if (components.isEmpty()) {
                log.info("No exact age-range match found. Falling back to '1-100' (All Ages).");

                String fallbackSql = "SELECT component_id, component_name, unit, normal_range, remarks, ageRange, gender " +
                        "FROM components " +
                        "WHERE panel_id = ? " +
                        "AND ageRange = '1-100' " +
                        "AND status = 'Active'";

                try (PreparedStatement stmt = conn.prepareStatement(fallbackSql)) {
                    stmt.setInt(1, panelId);

                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        String componentGender = rs.getString("gender");

                        Map<String, Object> component = buildComponentMap(rs);

                        // Apply gender prioritization even for fallback
                        if (!"Other".equalsIgnoreCase(gender) && componentGender.equalsIgnoreCase(gender)) {
                            genderSpecificMatches.add(component);
                        } else if ("All".equalsIgnoreCase(componentGender)) {
                            genderAllMatches.add(component);
                        }
                    }
                }

                components.addAll(genderSpecificMatches);
                components.addAll(genderAllMatches);
            }

            log.info("Retrieved " + components.size() + " components for panel_id: " + panelId +
                    ", age: " + patientAge + ", gender: " + gender);

        } catch (Exception e) {
            log.severe("Failed to retrieve components by age and gender: " + e.getMessage());
            throw e;
        }

        return components;
    }

    /**
     * Builds a component map from a ResultSet.
     *
     * @param rs The ResultSet
     * @return Component data map
     */
    private static Map<String, Object> buildComponentMap(ResultSet rs) throws Exception {
        Map<String, Object> component = new HashMap<>();
        component.put("component_id", rs.getInt("component_id"));
        component.put("component_name", rs.getString("component_name"));
        component.put("unit", rs.getString("unit"));
        component.put("normal_range", rs.getString("normal_range"));
        component.put("remarks", rs.getString("remarks"));
        component.put("age_range", rs.getString("ageRange"));
        component.put("gender", rs.getString("gender"));
        return component;
    }

    /**
     * Retrieves components from the database based on panel name, age, and gender.
     * Matches records where:
     * - panel_name matches the input
     * - age falls within the age_range (e.g., age=26 matches range "0-100", "0-50", "19-65", etc.)
     * - gender matches the input (or is 'All')
     * Falls back to "1-100" (all ages) if no exact age range match is found.
     *
     * @param panelName The panel name
     * @param ageStr    The patient age as a string number (e.g., "26")
     * @param gender    The gender
     * @return List of component records
     */
    private static List<Map<String, Object>> getComponentsByPanelAndDemographics(
            String panelName,
            String ageStr,
            String gender) throws Exception {

        List<Map<String, Object>> components = new ArrayList<>();

        try {
            // Parse age from string
            int patientAge = 0;
            try {
                patientAge = Integer.parseInt(ageStr != null ? ageStr.trim() : "0");
            } catch (NumberFormatException e) {
                log.warning("Invalid age value: " + ageStr + ", defaulting to 0");
                patientAge = 0;
            }

            Connection conn = DatabaseManager.getConnection();

            // First, try to get components with exact age range match (excluding all-ages fallback)
            String sql = "SELECT component_id, component_name, unit, normal_range, remarks, ageRange, gender " +
                    "FROM components " +
                    "WHERE panel_name = ? " +
                    "AND ageRange != '1-100' " +
                    "AND (gender = ? OR gender = 'All') " +
                    "AND status = 'Active' " +
                    "ORDER BY gender DESC, component_id";

            List<Map<String, Object>> exactMatches = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, panelName);
                stmt.setString(2, gender);

                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    String ageRange = rs.getString("ageRange");
                    
                    // Check if patient age falls within this age range
                    if (isAgeInRange(patientAge, ageRange)) {
                        Map<String, Object> component = new HashMap<>();
                        component.put("component_id", rs.getInt("component_id"));
                        component.put("component_name", rs.getString("component_name"));
                        component.put("unit", rs.getString("unit"));
                        component.put("normal_range", rs.getString("normal_range"));
                        component.put("remarks", rs.getString("remarks"));
                        component.put("age_range", rs.getString("ageRange"));
                        component.put("gender", rs.getString("gender"));

                        exactMatches.add(component);
                    }
                }
            }

            // If exact age range matches found, use them
            if (!exactMatches.isEmpty()) {
                components.addAll(exactMatches);
                log.info("Found " + components.size() + " components with exact age range match for panel: " + panelName +
                        ", age: " + patientAge + ", gender: " + gender);
            } else {
                // Fallback to "1-100" (all ages) range
                log.info("No exact age range match found. Falling back to 1-100 (all ages) for panel: " + panelName);
                
                String fallbackSql = "SELECT component_id, component_name, unit, normal_range, remarks, ageRange, gender " +
                        "FROM components " +
                        "WHERE panel_name = ? " +
                        "AND ageRange = '1-100' " +
                        "AND (gender = ? OR gender = 'All') " +
                        "AND status = 'Active' " +
                        "ORDER BY gender DESC, component_id";

                try (PreparedStatement stmt = conn.prepareStatement(fallbackSql)) {
                    stmt.setString(1, panelName);
                    stmt.setString(2, gender);

                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        Map<String, Object> component = new HashMap<>();
                        component.put("component_id", rs.getInt("component_id"));
                        component.put("component_name", rs.getString("component_name"));
                        component.put("unit", rs.getString("unit"));
                        component.put("normal_range", rs.getString("normal_range"));
                        component.put("remarks", rs.getString("remarks"));
                        component.put("age_range", rs.getString("ageRange"));
                        component.put("gender", rs.getString("gender"));

                        components.add(component);
                    }
                }
                
                log.info("Found " + components.size() + " fallback components (1-100 age range) for panel: " + panelName);
            }

        } catch (Exception e) {
            log.severe("Failed to retrieve components: " + e.getMessage());
            throw e;
        }

        return components;
    }

    /**
     * Checks if a patient age falls within an age range string (e.g., "0-18", "19-65", "0-100").
     *
     * @param patientAge The patient age in years
     * @param ageRange   The age range string
     * @return true if patient age is within the range (inclusive)
     */
    private static boolean isAgeInRange(int patientAge, String ageRange) {
        if (ageRange == null || ageRange.isEmpty()) {
            return false;
        }

        try {
            String[] parts = ageRange.split("-");
            if (parts.length != 2) {
                return false;
            }

            int minAge = Integer.parseInt(parts[0].trim());
            int maxAge = Integer.parseInt(parts[1].trim());

            boolean inRange = patientAge >= minAge && patientAge <= maxAge;
            log.fine("Age " + patientAge + " vs range " + ageRange + ": " + inRange);
            return inRange;
        } catch (NumberFormatException e) {
            log.warning("Invalid age range format: " + ageRange);
            return false;
        }
    }

    /**
     * Builds a single INSERT statement for a component with proper SQL escaping.
     *
     * @param testOrderId The test order ID
     * @param component   The component data
     * @return SQL INSERT statement as string
     */
    private static String buildInsertStatementWithAge(int testOrderId, Map<String, Object> component) {
        int componentId = (Integer) component.get("component_id");
        String componentName = escapeSQL((String) component.get("component_name"));
        String unit = escapeSQL((String) component.get("unit"));
        String normalRange = escapeSQL((String) component.get("normal_range"));
        String timestamp = LocalDateTime.now().toString();

        return "INSERT INTO test_order_component " +
                "(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) " +
                "VALUES (" +
                testOrderId + ", " +
                componentId + ", '" +
                componentName + "', '" +
                unit + "', '" +
                normalRange + "', " +
                "NULL, 'Normal', '" +
                timestamp + "')";
    }

    /**
     * Builds a single INSERT statement for a component.
     *
     * @param testOrderId The test order ID
     * @param component   The component data
     * @return SQL INSERT statement as string
     */
    private static String buildInsertStatement(int testOrderId, Map<String, Object> component) {
        int componentId = (Integer) component.get("component_id");
        String componentName = escapeSQL((String) component.get("component_name"));
        String unit = escapeSQL((String) component.get("unit"));
        String normalRange = escapeSQL((String) component.get("normal_range"));
        String timestamp = LocalDateTime.now().toString();

        return "INSERT INTO test_order_component " +
                "(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) " +
                "VALUES (" +
                testOrderId + ", " +
                componentId + ", '" +
                componentName + "', '" +
                unit + "', '" +
                normalRange + "', " +
                "NULL, 'Normal', '" +
                timestamp + "')";
    }

    /**
     * Escapes single quotes in SQL strings to prevent injection.
     *
     * @param str The string to escape
     * @return Escaped string
     */
    private static String escapeSQL(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("'", "''");
    }

    /**
     * Retrieves all test order components for a specific test order.
     *
     * @param testOrderId The test order ID
     * @return List of test order components
     */
    public static List<Map<String, Object>> getTestOrderComponents(int testOrderId) {
        List<Map<String, Object>> components = new ArrayList<>();

        try {
            Connection conn = DatabaseManager.getConnection();

            String sql = "SELECT id, test_order_id, component_id, component_name, unit, reference_range, " +
                    "result_value, flag, created_at " +
                    "FROM test_order_component " +
                    "WHERE test_order_id = ? " +
                    "ORDER BY component_id";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, testOrderId);

                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    Map<String, Object> component = new HashMap<>();
                    component.put("id", rs.getInt("id"));
                    component.put("test_order_id", rs.getInt("test_order_id"));
                    component.put("component_id", rs.getInt("component_id"));
                    component.put("component_name", rs.getString("component_name"));
                    component.put("unit", rs.getString("unit"));
                    component.put("reference_range", rs.getString("reference_range"));
                    component.put("result_value", rs.getString("result_value"));
                    component.put("flag", rs.getString("flag"));
                    component.put("created_at", rs.getString("created_at"));

                    components.add(component);
                }

                log.info("Retrieved " + components.size() + " components for test_order_id: " + testOrderId);
            }

        } catch (Exception e) {
            log.severe("Failed to retrieve test order components: " + e.getMessage());
            throw new RuntimeException(e);
        }

        return components;
    }

    /**
     * Updates a test order component result.
     *
     * @param componentId The test order component ID
     * @param resultValue The result value
     * @param flag        The result flag ('Normal', 'Abnormal', etc.)
     * @return true if update was successful
     */
    public static boolean updateComponentResult(int componentId, String resultValue, String flag) {
        try {
            Connection conn = DatabaseManager.getConnection();

            String sql = "UPDATE test_order_component " +
                    "SET result_value = ?, flag = ?, updated_at = datetime('now') " +
                    "WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, resultValue);
                stmt.setString(2, flag);
                stmt.setInt(3, componentId);

                int rowsAffected = stmt.executeUpdate();
                log.info("Updated component result for component_id: " + componentId +
                        ", rows affected: " + rowsAffected);

                return rowsAffected > 0;
            }

        } catch (Exception e) {
            log.severe("Failed to update component result: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Inserts a new test order component result row.
     *
     * @param testOrderId    The test order ID
     * @param componentId    The master component ID (optional)
     * @param componentName  The display component name
     * @param unit           The measurement unit
     * @param referenceRange The normal/reference range
     * @param resultValue    The entered result value
     * @param flag           The result flag
     * @return true if insert was successful
     */
    public static boolean insertComponentResult(int testOrderId, int componentId, String componentName, String unit, String referenceRange, String resultValue, String flag) {
        try {
            Connection conn = DatabaseManager.getConnection();
            String sql = "INSERT INTO test_order_component " +
                    "(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now'))";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, testOrderId);
                stmt.setInt(2, componentId);
                stmt.setString(3, componentName != null ? componentName : "");
                stmt.setString(4, unit != null ? unit : "");
                stmt.setString(5, referenceRange != null ? referenceRange : "");
                stmt.setString(6, resultValue != null ? resultValue : "");
                stmt.setString(7, flag != null ? flag : "Normal");

                int rowsAffected = stmt.executeUpdate();
                log.info("Inserted new test order component for test_order_id: " + testOrderId + ", rows affected: " + rowsAffected);
                return rowsAffected > 0;
            }
        } catch (Exception e) {
            log.severe("Failed to insert component result: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

        /**
         * Deletes a test_order_component row by its id.
         * @param componentId the id of the test_order_component row
         * @return true if a row was deleted
         */
        public static boolean deleteComponentById(int componentId) {
            try {
                Connection conn = DatabaseManager.getConnection();
                String sql = "DELETE FROM test_order_component WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, componentId);
                    int rows = stmt.executeUpdate();
                    log.info("Deleted test_order_component id=" + componentId + ", rows=" + rows);
                    return rows > 0;
                }
            } catch (Exception e) {
                log.severe("Failed to delete component id=" + componentId + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        }



        public static String escape(String value) {
            if (value == null) return "";
            return value.replace("\"", "\\\"");
        }

        public static String toJsonObject(Map<String, Object> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escape(entry.getKey())).append("\":");
                Object val = entry.getValue();
                if (val instanceof Number) {
                    sb.append(val.toString());
                } else {
                    sb.append("\"").append(escape(String.valueOf(val))).append("\"");
                }
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }

        public static String toJsonArray(List<String> objects) {
            return "[" + String.join(",", objects) + "]";
        }
}
