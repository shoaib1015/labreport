/**
 * Example test cases for TestOrderComponentService.generateInsertStatementsWithAge()
 */

import org.json.JSONArray;
import org.json.JSONObject;
import labreport.auth.TestOrderComponentService;

public class TestOrderComponentServiceExample {

    /**
     * Example 1: 5-year-old male patient taking CBC test
     */
    public static void example1_ChildMalePatient() {
        System.out.println("=== Example 1: Child (Age 5) Male Patient - CBC ===");

        String result = TestOrderComponentService.generateInsertStatementsWithAge(
            101,              // test_order_id
            1,                // patient_id
            "CBC",            // panel_name
            1,                // panel_id
            "2021-05-10",     // patient_dob (Calculated age: 5 years)
            "Male"            // gender
        );

        System.out.println("\nSQL Transaction:");
        System.out.println(result.getString("sqlTransaction"));

        System.out.println("\nInserted Components:");
        JSONArray components = result.getJSONArray("insertedComponents");
        for (int i = 0; i < components.length(); i++) {
            JSONObject comp = components.getJSONObject(i);
            System.out.printf("  - %s: %s %s (Age Range: %s, Gender: %s)\n",
                comp.get("component_id"),
                comp.get("component_name"),
                comp.get("unit"),
                comp.get("age_range"),
                comp.get("gender")
            );
        }
    }

    /**
     * Example 2: 35-year-old female patient taking Lipid Profile test
     */
    public static void example2_AdultFemalePatient() {
        System.out.println("\n=== Example 2: Adult (Age 35) Female Patient - Lipid Profile ===");

        String result = TestOrderComponentService.generateInsertStatementsWithAge(
            102,              // test_order_id
            2,                // patient_id
            "Lipid Profile",  // panel_name
            2,                // panel_id
            "1988-12-15",     // patient_dob (Calculated age: 35 years)
            "Female"          // gender
        );

        System.out.println("\nSQL Transaction:");
        System.out.println(result.getString("sqlTransaction"));

        System.out.println("\nInserted Components:");
        JSONArray components = result.getJSONArray("insertedComponents");
        for (int i = 0; i < components.length(); i++) {
            JSONObject comp = components.getJSONObject(i);
            System.out.printf("  - %s: %s (Range: %s)\n",
                comp.get("component_id"),
                comp.get("component_name"),
                comp.get("reference_range")
            );
        }
    }

    /**
     * Example 3: 70-year-old patient with "Other" gender taking LFT test
     */
    public static void example3_SeniorOtherGenderPatient() {
        System.out.println("\n=== Example 3: Senior (Age 70) Other Gender - Liver Function Test ===");

        String result = TestOrderComponentService.generateInsertStatementsWithAge(
            103,                        // test_order_id
            3,                          // patient_id
            "Liver Function Test",      // panel_name
            3,                          // panel_id
            "1954-05-28",               // patient_dob (Calculated age: 70 years)
            "Other"                     // gender
        );

        System.out.println("\nSQL Transaction:");
        System.out.println(result.getString("sqlTransaction"));

        System.out.println("\nInserted Components (should only have 'All' gender):");
        JSONArray components = result.getJSONArray("insertedComponents");
        for (int i = 0; i < components.length(); i++) {
            JSONObject comp = components.getJSONObject(i);
            System.out.printf("  - Gender: %s (Expected: 'All')\n", comp.get("gender"));
        }
    }

    /**
     * Example 4: Fallback to "1-100" age range when no exact match
     */
    public static void example4_FallbackToAllAges() {
        System.out.println("\n=== Example 4: Age 100+ Patient (Fallback to 1-100) ===");

        String result = TestOrderComponentService.generateInsertStatementsWithAge(
            104,              // test_order_id
            4,                // patient_id
            "CBC",            // panel_name
            1,                // panel_id
            "1920-01-01",     // patient_dob (Calculated age: 100+ years)
            "Male"            // gender
        );

        System.out.println("\nNote: If no components found for exact age range (e.g., 0-18, 19-65),");
        System.out.println("the query falls back to age_range = '1-100'.");
        System.out.println("\nInserted Components:");
        JSONArray components = result.getJSONArray("insertedComponents");
        System.out.println("Total components: " + components.length());
        for (int i = 0; i < components.length(); i++) {
            JSONObject comp = components.getJSONObject(i);
            System.out.printf("  - %s (Age Range: %s)\n", 
                comp.get("component_name"),
                comp.get("age_range")
            );
        }
    }

    /**
     * Example 5: Extract and execute SQL transaction
     */
    public static void example5_ExecuteSQLTransaction() {
        System.out.println("\n=== Example 5: Execute SQL Transaction ===");

        String result = TestOrderComponentService.generateInsertStatementsWithAge(
            105,              // test_order_id
            5,                // patient_id
            "CBC",            // panel_name
            1,                // panel_id
            "2020-06-15",     // patient_dob
            "Male"            // gender
        );

        String sqlTransaction = result.getString("sqlTransaction");

        System.out.println("SQL Transaction to execute:");
        System.out.println(sqlTransaction);

        System.out.println("\n--- Execution Code Example ---");
        System.out.println("""
            Connection conn = DatabaseManager.getConnection();
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sqlTransaction);
                System.out.println("Transaction executed successfully!");
            } catch (SQLException e) {
                System.err.println("Transaction failed: " + e.getMessage());
            }
        """);
    }

    /**
     * Example 6: Gender Prioritization Demonstration
     */
    public static void example6_GenderPrioritization() {
        System.out.println("\n=== Example 6: Gender Prioritization Rules ===");

        System.out.println("\n--- Priority for Male Patient ---");
        String maleResult = TestOrderComponentService.generateInsertStatementsWithAge(
            106, 6, "CBC", 1, "2000-01-01", "Male"
        );
        JSONArray maleComps = new JSONArray(maleResult).getJSONArray("insertedComponents");
        System.out.println("Components for male patient:");
        maleComps.forEach(comp -> {
            JSONObject c = (JSONObject) comp;
            System.out.printf("  - %s (Gender: %s)\n", c.get("component_name"), c.get("gender"));
        });

        System.out.println("\n--- Priority for Female Patient ---");
        String femaleResult = TestOrderComponentService.generateInsertStatementsWithAge(
            107, 7, "CBC", 1, "2000-01-01", "Female"
        );
        JSONArray femaleComps = new JSONArray(femaleResult).getJSONArray("insertedComponents");
        System.out.println("Components for female patient:");
        femaleComps.forEach(comp -> {
            JSONObject c = (JSONObject) comp;
            System.out.printf("  - %s (Gender: %s)\n", c.get("component_name"), c.get("gender"));
        });

        System.out.println("\n--- Priority for Other Gender Patient ---");
        String otherResult = TestOrderComponentService.generateInsertStatementsWithAge(
            108, 8, "CBC", 1, "2000-01-01", "Other"
        );
        JSONArray otherComps = new JSONArray(otherResult).getJSONArray("insertedComponents");
        System.out.println("Components for other gender patient:");
        otherComps.forEach(comp -> {
            JSONObject c = (JSONObject) comp;
            System.out.printf("  - %s (Gender: %s) - Should be 'All'\n", 
                c.get("component_name"), c.get("gender"));
        });
    }

    /**
     * Example 7: JSON Output Structure
     */
    public static void example7_JSONStructure() {
        System.out.println("\n=== Example 7: JSON Output Structure ===");

        String result = TestOrderComponentService.generateInsertStatementsWithAge(
            109, 9, "CBC", 1, "2010-03-20", "Male"
        );

        System.out.println("JSON Structure:");
        System.out.println(new JSONObject(result).toString(2));  // Pretty print with 2-space indent

        System.out.println("\nAccessing Individual Fields:");
        System.out.println("- sqlTransaction: " + new JSONObject(result).getString("sqlTransaction").substring(0, 50) + "...");
        System.out.println("- insertedComponents count: " + 
            new JSONObject(result).getJSONArray("insertedComponents").length());

        System.out.println("\nAccessing Component Details:");
        JSONArray comps = new JSONObject(result).getJSONArray("insertedComponents");
        if (comps.length() > 0) {
            JSONObject firstComp = comps.getJSONObject(0);
            System.out.println("  - component_id: " + firstComp.get("component_id"));
            System.out.println("  - component_name: " + firstComp.get("component_name"));
            System.out.println("  - unit: " + firstComp.get("unit"));
            System.out.println("  - reference_range: " + firstComp.get("reference_range"));
            System.out.println("  - age_range: " + firstComp.get("age_range"));
            System.out.println("  - gender: " + firstComp.get("gender"));
        }
    }

    /**
     * Main method to run all examples
     */
    public static void main(String[] args) {
        try {
            example1_ChildMalePatient();
            example2_AdultFemalePatient();
            example3_SeniorOtherGenderPatient();
            example4_FallbackToAllAges();
            example5_ExecuteSQLTransaction();
            example6_GenderPrioritization();
            example7_JSONStructure();

            System.out.println("\n" + "=".repeat(60));
            System.out.println("All examples completed successfully!");
            System.out.println("=".repeat(60));

        } catch (Exception e) {
            System.err.println("Error running examples: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
