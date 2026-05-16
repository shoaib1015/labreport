# Test Order Component Service Documentation

## Overview
This documentation explains the implementation of the SQL INSERT statement generation system for the `test_order_component` table. The system automatically generates and inserts test order components based on patient demographics (age group and gender).

## Database Schema

### test_order_component Table
```sql
CREATE TABLE test_order_component (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    test_order_id INTEGER NOT NULL,
    component_id INTEGER NOT NULL,
    component_name TEXT NOT NULL,
    unit TEXT,
    reference_range TEXT,
    result_value TEXT,
    flag TEXT DEFAULT 'Normal',
    created_at TEXT DEFAULT (datetime('now')),
    updated_at TEXT,
    FOREIGN KEY(test_order_id) REFERENCES test_order(id),
    FOREIGN KEY(component_id) REFERENCES components(component_id)
);
```

## Input Parameters

| Parameter | Type | Required | Description | Valid Values |
|-----------|------|----------|-------------|--------------|
| `test_order_id` | integer | Yes | Unique test order identifier | Any positive integer |
| `patient_id` | integer | Yes | Associated patient ID | Any positive integer |
| `panel_name` | string | Yes | Name of the test panel | 'CBC', 'Lipid Profile', 'Liver Function Test' |
| `panel_id` | integer | Yes | Associated panel ID | Any positive integer |
| `age_group` | string | Yes | Patient age group | 'Child', 'Adult' |
| `gender` | string | Yes | Patient gender | 'Male', 'Female', 'Other' |

## Service Methods

### 1. generateInsertStatements()
Generates SQL INSERT statements as strings without executing them.

**Signature:**
```java
public static List<String> generateInsertStatements(
    int testOrderId,
    int patientId,
    String panelName,
    int panelId,
    String ageGroup,
    String gender)
```

**Returns:** `List<String>` - List of SQL INSERT statements

**Use Case:** When you need to review or log the SQL statements before execution.

**Example:**
```java
List<String> statements = TestOrderComponentService.generateInsertStatements(
    1,           // test_order_id
    101,         // patient_id
    "CBC",       // panel_name
    10,          // panel_id
    "Adult",     // age_group
    "Male"       // gender
);

// Output:
// INSERT INTO test_order_component (test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) VALUES (1, 1, 'Hemoglobin', 'g/dL', '13.5-17.5 g/dL', NULL, 'Normal', '2026-05-14T...');
// INSERT INTO test_order_component (test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) VALUES (1, 2, 'WBC Count', 'cells/µL', '4000-11000 cells/µL', NULL, 'Normal', '2026-05-14T...');
// ... more statements
```

### 2. insertTestOrderComponents()
Directly inserts test order components into the database.

**Signature:**
```java
public static int insertTestOrderComponents(
    int testOrderId,
    int patientId,
    String panelName,
    int panelId,
    String ageGroup,
    String gender)
```

**Returns:** `int` - Number of rows inserted

**Use Case:** Production scenario where components need to be immediately added to the database.

**Example:**
```java
int rowsInserted = TestOrderComponentService.insertTestOrderComponents(
    2,              // test_order_id
    102,            // patient_id
    "Lipid Profile", // panel_name
    11,             // panel_id
    "Adult",        // age_group
    "Female"        // gender
);

System.out.println("Inserted " + rowsInserted + " components");
// Output: Inserted 5 components
```

### 3. getTestOrderComponents()
Retrieves all components for a specific test order.

**Signature:**
```java
public static List<Map<String, Object>> getTestOrderComponents(int testOrderId)
```

**Returns:** `List<Map<String, Object>>` - List of component records

**Example:**
```java
List<Map<String, Object>> components = TestOrderComponentService.getTestOrderComponents(1);

for (Map<String, Object> component : components) {
    System.out.println("Component: " + component.get("component_name"));
    System.out.println("  Result: " + component.get("result_value"));
    System.out.println("  Flag: " + component.get("flag"));
}
```

### 4. updateComponentResult()
Updates the result and flag for a specific component.

**Signature:**
```java
public static boolean updateComponentResult(int componentId, String resultValue, String flag)
```

**Returns:** `boolean` - true if update was successful

**Parameters:**
- `componentId`: ID from test_order_component table
- `resultValue`: The numerical or text result
- `flag`: Status ('Normal', 'Abnormal', 'Critical', etc.)

**Example:**
```java
boolean success = TestOrderComponentService.updateComponentResult(
    5,              // component ID
    "7.5 g/dL",    // result value
    "Normal"        // flag
);

if (success) {
    System.out.println("Result updated successfully");
}
```

## Component Matching Logic

The service queries the `components` table using the following SQL:

```sql
SELECT component_id, component_name, unit, normal_range, remarks, ageRange, gender
FROM components
WHERE panel_name = ?
AND (ageRange = ? OR ageRange = 'All')
AND (gender = ? OR gender = 'All')
AND status = 'Active'
ORDER BY component_id
```

**Key Points:**
- **Exact Match:** `panel_name` must match exactly
- **Flexible Age Group:** Matches specific age group OR 'All'
- **Flexible Gender:** Matches specific gender OR 'All'
- **Active Only:** Only active components are included
- **Ordered:** Results are ordered by component_id

## Integration with Existing Code

### Example 1: Integrating with PatientHandler
```java
// In PatientHandler.java
public void handleTestOrder(Map<String, String> params) throws Exception {
    int testOrderId = Integer.parseInt(params.get("test_order_id"));
    int patientId = Integer.parseInt(params.get("patient_id"));
    String panelName = params.get("panel_name");
    int panelId = Integer.parseInt(params.get("panel_id"));
    String ageGroup = params.get("age_group");  // from patient DOB calculation
    String gender = params.get("gender");

    try {
        int rowsInserted = TestOrderComponentService.insertTestOrderComponents(
            testOrderId, patientId, panelName, panelId, ageGroup, gender
        );
        
        // Return success response
        return "{ \"status\": \"success\", \"components_inserted\": " + rowsInserted + " }";
    } catch (Exception e) {
        // Return error response
        return "{ \"status\": \"error\", \"message\": \"" + e.getMessage() + "\" }";
    }
}
```

### Example 2: Integrating with SecureTestHandler
```java
// In SecureTestHandler.java
@Override
public void handle(HttpExchange exchange) throws IOException {
    Map<String, String> params = parseRequest(exchange);
    
    // Extract parameters
    int testOrderId = Integer.parseInt(params.get("test_order_id"));
    int patientId = Integer.parseInt(params.get("patient_id"));
    String panelName = params.get("panel_name");
    int panelId = Integer.parseInt(params.get("panel_id"));
    String ageGroup = determineAgeGroup(patientId);  // Calculate from DOB
    String gender = params.get("gender");
    
    // Insert components
    int result = TestOrderComponentService.insertTestOrderComponents(
        testOrderId, patientId, panelName, panelId, ageGroup, gender
    );
    
    // Send response
    sendJsonResponse(exchange, 200, 
        "{ \"success\": true, \"components_created\": " + result + " }");
}
```

## Common Usage Scenarios

### Scenario 1: Create Adult CBC Panel
```java
TestOrderComponentService.insertTestOrderComponents(
    100,        // test_order_id
    50,         // patient_id
    "CBC",      // panel_name
    10,         // panel_id
    "Adult",    // age_group
    "Male"      // gender
);
```

Expected result: Components for Adult Males with CBC panel are inserted.

### Scenario 2: Create Child Lipid Profile
```java
TestOrderComponentService.insertTestOrderComponents(
    101,
    51,
    "Lipid Profile",
    11,
    "Child",
    "Female"
);
```

Expected result: Components for Children (both genders if 'All' specified) are inserted.

### Scenario 3: Generate Statements for Audit
```java
List<String> statements = TestOrderComponentService.generateInsertStatements(
    102, 52, "Liver Function Test", 12, "Adult", "Other"
);

// Log all statements for audit trail
for (String sql : statements) {
    AppLogger.getLogger().info("Generated SQL: " + sql);
}
```

## Error Handling

The service includes comprehensive error handling:

```java
try {
    int result = TestOrderComponentService.insertTestOrderComponents(
        testOrderId, patientId, panelName, panelId, ageGroup, gender
    );
} catch (RuntimeException e) {
    // Service wraps exceptions in RuntimeException
    System.err.println("Error: " + e.getMessage());
    // Log the error
    AppLogger.getLogger().log(Level.SEVERE, "Failed to insert components", e);
    // Return error to client
}
```

Common exceptions:
- `NullPointerException`: Missing or null parameters
- `SQLException`: Database connection or query errors
- `NumberFormatException`: Invalid integer parameters

## Testing

Run the TestOrderComponentExample class to test the service:

```bash
javac TestOrderComponentExample.java
java labreport.auth.TestOrderComponentExample
```

This will execute three examples demonstrating different use cases.

## Database Notes

- **Timestamps:** Created with `datetime('now')` in SQLite format
- **Foreign Keys:** Require corresponding entries in `test_order` and `components` tables
- **Result Value:** Initially NULL, filled when test results are entered
- **Flag:** Defaults to 'Normal', can be updated to 'Abnormal', 'Critical', etc.
- **Updated At:** Automatically updated when component result is modified

## Performance Considerations

- **Query Optimization:** Uses indexed lookups on panel_name and ageRange
- **Batch Operations:** Consider using transactions for multiple insertions
- **Component Cache:** Consider caching frequently accessed components

## Future Enhancements

Potential improvements:
1. Batch insertion using transactions
2. Component result validation rules
3. Normal range comparison and automatic flagging
4. Component archival/deletion management
5. Duplicate component prevention
6. Component reordering/priority handling
