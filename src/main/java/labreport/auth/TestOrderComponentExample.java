package labreport.auth;

import java.util.List;
import java.util.Map;

/**
 * Example usage of TestOrderComponentService.
 * Demonstrates how to generate and insert test order components.
 */
public class TestOrderComponentExample {

    public static void main(String[] args) {
        // Example 1: Generate INSERT statements without inserting
        generateInsertStatementsExample();

        // Example 2: Directly insert components into database
        insertComponentsExample();

        // Example 3: Retrieve and update components
        retrieveAndUpdateExample();
    }

    /**
     * Example: Generate INSERT statements for review
     */
    public static void generateInsertStatementsExample() {
        System.out.println("\n=== Example 1: Generate INSERT Statements ===\n");

        int testOrderId = 1;
        int patientId = 101;
        String panelName = "CBC";  // Complete Blood Count
        int panelId = 10;
        String ageGroup = "Adult";
        String gender = "Male";

        try {
            List<String> sqlStatements = TestOrderComponentService.generateInsertStatements(
                    testOrderId,
                    patientId,
                    panelName,
                    panelId,
                    ageGroup,
                    gender
            );

            System.out.println("Generated " + sqlStatements.size() + " INSERT statements:\n");
            for (int i = 0; i < sqlStatements.size(); i++) {
                System.out.println((i + 1) + ". " + sqlStatements.get(i) + ";");
            }

        } catch (Exception e) {
            System.err.println("Error generating statements: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example: Directly insert components into the database
     */
    public static void insertComponentsExample() {
        System.out.println("\n=== Example 2: Insert Components into Database ===\n");

        int testOrderId = 2;
        int patientId = 102;
        String panelName = "Lipid Profile";
        int panelId = 11;
        String ageGroup = "Adult";
        String gender = "Female";

        try {
            int rowsInserted = TestOrderComponentService.insertTestOrderComponents(
                    testOrderId,
                    patientId,
                    panelName,
                    panelId,
                    ageGroup,
                    gender
            );

            System.out.println("Successfully inserted " + rowsInserted + " components for test_order_id: " + testOrderId);

        } catch (Exception e) {
            System.err.println("Error inserting components: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example: Retrieve and update component results
     */
    public static void retrieveAndUpdateExample() {
        System.out.println("\n=== Example 3: Retrieve and Update Components ===\n");

        int testOrderId = 1;

        try {
            // Retrieve all components for a test order
            List<Map<String, Object>> components = TestOrderComponentService.getTestOrderComponents(testOrderId);

            System.out.println("Retrieved " + components.size() + " components for test_order_id: " + testOrderId + "\n");

            for (Map<String, Object> component : components) {
                System.out.println("Component: " + component.get("component_name"));
                System.out.println("  - Unit: " + component.get("unit"));
                System.out.println("  - Reference Range: " + component.get("reference_range"));
                System.out.println("  - Result: " + component.get("result_value"));
                System.out.println("  - Flag: " + component.get("flag"));
                System.out.println();
            }

            // Update result for first component (if exists)
            if (!components.isEmpty()) {
                int componentId = (Integer) components.get(0).get("id");
                String resultValue = "7.5 g/dL";
                String flag = "Normal";

                boolean updated = TestOrderComponentService.updateComponentResult(
                        componentId,
                        resultValue,
                        flag
                );

                if (updated) {
                    System.out.println("Successfully updated component result for ID: " + componentId);
                } else {
                    System.out.println("Failed to update component result");
                }
            }

        } catch (Exception e) {
            System.err.println("Error retrieving/updating components: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Usage Example for integrating with existing handlers
     */
    public static void handlerIntegrationExample() {
        // This can be called from a handler like PatientHandler, SecureTestHandler, etc.

        // Parameters from HTTP request
        int testOrderId = 5;
        int patientId = 103;
        String panelName = "Liver Function Test";
        int panelId = 12;
        String ageGroup = "Child";
        String gender = "Other";

        try {
            // Option 1: Insert directly (recommended for immediate action)
            int rowsInserted = TestOrderComponentService.insertTestOrderComponents(
                    testOrderId,
                    patientId,
                    panelName,
                    panelId,
                    ageGroup,
                    gender
            );

            System.out.println("Test order components created: " + rowsInserted + " components");

            // Option 2: Generate statements for logging/audit trail
            List<String> sqlStatements = TestOrderComponentService.generateInsertStatements(
                    testOrderId,
                    patientId,
                    panelName,
                    panelId,
                    ageGroup,
                    gender
            );

            // Log the statements
            for (String sql : sqlStatements) {
                System.out.println("SQL: " + sql);
            }

        } catch (Exception e) {
            System.err.println("Error processing test order: " + e.getMessage());
            // Return error response to client
        }
    }
}
