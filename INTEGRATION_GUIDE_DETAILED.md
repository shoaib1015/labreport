-- =====================================================
-- Integration Guide for generateInsertStatementsWithAge()
-- =====================================================

## Quick Start

### Method Signature
```java
public static JSONObject generateInsertStatementsWithAge(
    int testOrderId,
    int patientId,
    String panelName,
    int panelId,
    String patientDob,
    String gender
)
```

### Returns
```json
{
  "sqlTransaction": "BEGIN; INSERT INTO ...; COMMIT;",
  "insertedComponents": [
    {
      "component_id": 1,
      "component_name": "Hemoglobin",
      "unit": "g/dL",
      "reference_range": "11-14",
      "age_range": "0-18",
      "gender": "Male"
    }
  ]
}
```

---

## Integration Pattern 1: REST API Endpoint (POST)

### Endpoint Definition
```java
@POST
@Path("/test-orders/{testOrderId}/components/generate")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response generateTestOrderComponents(
        @PathParam("testOrderId") int testOrderId,
        ComponentGenerationRequest request) {
    
    try {
        // Validate input
        if (!isValidDate(request.getPatientDob())) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"Invalid DOB format. Use YYYY-MM-DD\"}")
                .build();
        }
        
        if (!isValidGender(request.getGender())) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("{\"error\": \"Invalid gender. Use Male, Female, or Other\"}")
                .build();
        }
        
        // Generate SQL and components
        JSONObject result = TestOrderComponentService.generateInsertStatementsWithAge(
            testOrderId,
            request.getPatientId(),
            request.getPanelName(),
            request.getPanelId(),
            request.getPatientDob(),
            request.getGender()
        );
        
        // Execute transaction
        executeSQLTransaction(result.getString("sqlTransaction"));
        
        // Return response
        return Response.ok(result.toString()).build();
        
    } catch (DateTimeParseException e) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("{\"error\": \"Invalid date format\"}")
            .build();
    } catch (Exception e) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity("{\"error\": \"" + e.getMessage() + "\"}")
            .build();
    }
}
```

### Request Class
```java
public class ComponentGenerationRequest {
    private int patientId;
    private String panelName;
    private int panelId;
    private String patientDob;      // Format: YYYY-MM-DD
    private String gender;          // Male, Female, or Other
    
    // Getters and setters...
}
```

### Example Request
```bash
POST /test-orders/101/components/generate
Content-Type: application/json

{
  "patientId": 1,
  "panelName": "CBC",
  "panelId": 1,
  "patientDob": "2021-05-10",
  "gender": "Male"
}
```

### Example Response
```json
{
  "sqlTransaction": "BEGIN;\nINSERT INTO test_order_component (test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at)\nVALUES (101, 1, 'Hemoglobin', 'g/dL', '11-14', NULL, 'Normal', '2026-05-28T14:30:45.123');\nINSERT INTO test_order_component (test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at)\nVALUES (101, 6, 'WBC Count', 'cells/µL', '5000-12000', NULL, 'Normal', '2026-05-28T14:30:45.123');\nCOMMIT;",
  "insertedComponents": [
    {
      "component_id": 1,
      "component_name": "Hemoglobin",
      "unit": "g/dL",
      "reference_range": "11-14",
      "age_range": "0-18",
      "gender": "Male"
    },
    {
      "component_id": 6,
      "component_name": "WBC Count",
      "unit": "cells/µL",
      "reference_range": "5000-12000",
      "age_range": "0-18",
      "gender": "All"
    }
  ]
}
```

---

## Integration Pattern 2: Direct Java Method Call

### Simple Usage
```java
public class TestOrderHandler {
    
    public void createTestOrder(TestOrder order) {
        // Generate components
        JSONObject result = TestOrderComponentService.generateInsertStatementsWithAge(
            order.getTestOrderId(),
            order.getPatientId(),
            order.getPanelName(),
            order.getPanelId(),
            order.getPatientDob().toString(),  // YYYY-MM-DD format
            order.getGender()
        );
        
        // Log the SQL
        System.out.println("Generated SQL:");
        System.out.println(result.getString("sqlTransaction"));
        
        // Access components for frontend display
        JSONArray components = result.getJSONArray("insertedComponents");
        components.forEach(comp -> {
            JSONObject c = (JSONObject) comp;
            System.out.println("Component: " + c.getString("component_name"));
        });
    }
}
```

### With Error Handling
```java
try {
    JSONObject result = TestOrderComponentService.generateInsertStatementsWithAge(
        101, 1, "CBC", 1, "2021-05-10", "Male"
    );
    
    String sqlTransaction = result.getString("sqlTransaction");
    JSONArray insertedComponents = result.getJSONArray("insertedComponents");
    
    // Verify results
    if (insertedComponents.length() == 0) {
        System.err.println("No components generated!");
        return;
    }
    
    // Display to user
    displayComponentsToUI(insertedComponents);
    
} catch (DateTimeParseException e) {
    System.err.println("Invalid date format: " + e.getMessage());
} catch (JSONException e) {
    System.err.println("JSON parsing error: " + e.getMessage());
} catch (Exception e) {
    System.err.println("Error: " + e.getMessage());
}
```

---

## Integration Pattern 3: Frontend/JavaScript Integration

### HTML Form
```html
<form id="componentGeneratorForm">
    <input type="number" name="testOrderId" placeholder="Test Order ID" required>
    <input type="number" name="patientId" placeholder="Patient ID" required>
    <input type="text" name="panelName" placeholder="Panel Name" required>
    <input type="number" name="panelId" placeholder="Panel ID" required>
    <input type="date" name="patientDob" required>
    <select name="gender" required>
        <option value="">Select Gender</option>
        <option value="Male">Male</option>
        <option value="Female">Female</option>
        <option value="Other">Other</option>
    </select>
    <button type="submit">Generate Components</button>
</form>

<div id="resultsContainer"></div>
```

### JavaScript Code
```javascript
document.getElementById('componentGeneratorForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const formData = new FormData(e.target);
    const testOrderId = parseInt(formData.get('testOrderId'));
    
    const request = {
        patientId: parseInt(formData.get('patientId')),
        panelName: formData.get('panelName'),
        panelId: parseInt(formData.get('panelId')),
        patientDob: formData.get('patientDob'),  // HTML5 date picker returns YYYY-MM-DD
        gender: formData.get('gender')
    };
    
    try {
        const response = await fetch(`/api/test-orders/${testOrderId}/components/generate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(request)
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        displayResults(data);
        
    } catch (error) {
        console.error('Error generating components:', error);
        showError(error.message);
    }
});

function displayResults(data) {
    const container = document.getElementById('resultsContainer');
    
    // Show SQL Transaction
    const sqlSection = document.createElement('div');
    sqlSection.innerHTML = `
        <h3>Generated SQL Transaction</h3>
        <pre><code>${escapeHtml(data.sqlTransaction)}</code></pre>
        <button onclick="copySQLToClipboard('${data.sqlTransaction}')">Copy SQL</button>
    `;
    container.appendChild(sqlSection);
    
    // Show Components Table
    const componentsSection = document.createElement('div');
    let html = `
        <h3>Inserted Components (${data.insertedComponents.length})</h3>
        <table border="1">
            <tr>
                <th>ID</th>
                <th>Component Name</th>
                <th>Unit</th>
                <th>Reference Range</th>
                <th>Age Range</th>
                <th>Gender</th>
            </tr>
    `;
    
    data.insertedComponents.forEach(comp => {
        html += `
            <tr>
                <td>${comp.component_id}</td>
                <td>${comp.component_name}</td>
                <td>${comp.unit}</td>
                <td>${comp.reference_range}</td>
                <td>${comp.age_range}</td>
                <td>${comp.gender}</td>
            </tr>
        `;
    });
    
    html += `</table>`;
    componentsSection.innerHTML = html;
    container.appendChild(componentsSection);
}

function copySQLToClipboard(sql) {
    navigator.clipboard.writeText(sql).then(() => {
        alert('SQL copied to clipboard!');
    });
}

function showError(message) {
    const container = document.getElementById('resultsContainer');
    container.innerHTML = `<div class="error">Error: ${escapeHtml(message)}</div>`;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
```

---

## Integration Pattern 4: Batch Processing

### Process Multiple Patients
```java
public class BatchComponentGenerator {
    
    public void generateComponentsForMultiplePatients(List<TestOrder> orders) {
        StringBuilder allTransactions = new StringBuilder();
        List<JSONObject> allResults = new ArrayList<>();
        
        for (TestOrder order : orders) {
            try {
                JSONObject result = TestOrderComponentService.generateInsertStatementsWithAge(
                    order.getTestOrderId(),
                    order.getPatientId(),
                    order.getPanelName(),
                    order.getPanelId(),
                    order.getPatientDob().toString(),
                    order.getGender()
                );
                
                // Append to batch
                String transaction = result.getString("sqlTransaction");
                
                // Remove BEGIN/COMMIT for batch
                String lines = transaction.replace("BEGIN;", "")
                                         .replace("COMMIT;", "")
                                         .trim();
                
                if (!lines.isEmpty()) {
                    allTransactions.append(lines).append("\n");
                }
                
                allResults.add(result);
                
                System.out.println("✓ Generated components for test order: " + order.getTestOrderId());
                
            } catch (Exception e) {
                System.err.println("✗ Failed for test order " + order.getTestOrderId() + 
                                 ": " + e.getMessage());
            }
        }
        
        // Execute single batch transaction
        if (allTransactions.length() > 0) {
            String batchTransaction = "BEGIN;\n" + allTransactions.toString() + "\nCOMMIT;";
            executeBatchTransaction(batchTransaction);
            System.out.println("Batch transaction executed for " + orders.size() + " orders");
        }
    }
    
    private void executeBatchTransaction(String transaction) {
        try {
            Connection conn = DatabaseManager.getConnection();
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(transaction);
            }
        } catch (SQLException e) {
            System.err.println("Batch transaction failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
```

---

## Integration Pattern 5: Error Handling & Validation

### Comprehensive Validation
```java
public class ComponentGenerationValidator {
    
    public static void validateInput(
            int testOrderId,
            int patientId,
            String panelName,
            int panelId,
            String patientDob,
            String gender) throws IllegalArgumentException {
        
        // Validate test order
        if (testOrderId <= 0) {
            throw new IllegalArgumentException("Invalid test_order_id: must be > 0");
        }
        
        // Validate patient
        if (patientId <= 0) {
            throw new IllegalArgumentException("Invalid patient_id: must be > 0");
        }
        
        // Validate panel name
        if (panelName == null || panelName.trim().isEmpty()) {
            throw new IllegalArgumentException("Panel name cannot be empty");
        }
        
        // Validate panel ID
        if (panelId <= 0) {
            throw new IllegalArgumentException("Invalid panel_id: must be > 0");
        }
        
        // Validate DOB format
        try {
            LocalDate dob = LocalDate.parse(patientDob);
            if (dob.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("DOB cannot be in the future");
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid DOB format: use YYYY-MM-DD");
        }
        
        // Validate gender
        if (!gender.equals("Male") && !gender.equals("Female") && !gender.equals("Other")) {
            throw new IllegalArgumentException("Gender must be Male, Female, or Other");
        }
    }
    
    public static void validateResult(JSONObject result) throws IllegalArgumentException {
        if (result == null) {
            throw new IllegalArgumentException("Result is null");
        }
        
        if (!result.has("sqlTransaction") || !result.has("insertedComponents")) {
            throw new IllegalArgumentException("Invalid result structure");
        }
        
        JSONArray components = result.getJSONArray("insertedComponents");
        if (components.length() == 0) {
            throw new IllegalArgumentException("No components generated for the given criteria");
        }
    }
}
```

---

## Database Execution Methods

### Method 1: Direct SQL Execution
```java
public static void executeSQLTransaction(String sqlTransaction) {
    try {
        Connection conn = DatabaseManager.getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sqlTransaction);
            System.out.println("Transaction executed successfully");
        }
    } catch (SQLException e) {
        System.err.println("Failed to execute transaction: " + e.getMessage());
        throw new RuntimeException(e);
    }
}
```

### Method 2: With Rollback on Error
```java
public static boolean executeSQLTransactionWithRollback(String sqlTransaction) {
    Connection conn = null;
    try {
        conn = DatabaseManager.getConnection();
        conn.setAutoCommit(false);
        
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sqlTransaction);
            conn.commit();
            return true;
        } catch (SQLException e) {
            conn.rollback();
            System.err.println("Transaction rolled back: " + e.getMessage());
            return false;
        }
    } catch (SQLException e) {
        System.err.println("Connection error: " + e.getMessage());
        return false;
    } finally {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
}
```

### Method 3: Prepared Statement (Alternative)
```java
public static int executeComponentInsertions(JSONArray components, int testOrderId) {
    int totalInserted = 0;
    
    try {
        Connection conn = DatabaseManager.getConnection();
        String insertSql = "INSERT INTO test_order_component " +
                "(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) " +
                "VALUES (?, ?, ?, ?, ?, NULL, 'Normal', datetime('now'))";
        
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            for (int i = 0; i < components.length(); i++) {
                JSONObject comp = components.getJSONObject(i);
                
                stmt.setInt(1, testOrderId);
                stmt.setInt(2, comp.getInt("component_id"));
                stmt.setString(3, comp.getString("component_name"));
                stmt.setString(4, comp.getString("unit"));
                stmt.setString(5, comp.getString("reference_range"));
                
                totalInserted += stmt.executeUpdate();
            }
        }
        
        System.out.println("Inserted " + totalInserted + " components");
        return totalInserted;
        
    } catch (SQLException e) {
        System.err.println("Insert failed: " + e.getMessage());
        throw new RuntimeException(e);
    }
}
```

---

## Testing & Verification

### Unit Test Example
```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestOrderComponentServiceTest {
    
    @Test
    public void testGenerateComponentsForChildMale() {
        JSONObject result = TestOrderComponentService.generateInsertStatementsWithAge(
            101, 1, "CBC", 1, "2021-05-10", "Male"
        );
        
        assertNotNull(result);
        assertTrue(result.has("sqlTransaction"));
        assertTrue(result.has("insertedComponents"));
        
        JSONArray components = result.getJSONArray("insertedComponents");
        assertTrue(components.length() > 0);
        
        // Verify that male components are prioritized
        boolean hasMaleComponent = false;
        for (int i = 0; i < components.length(); i++) {
            String gender = components.getJSONObject(i).getString("gender");
            if ("Male".equals(gender)) {
                hasMaleComponent = true;
                break;
            }
        }
        assertTrue(hasMaleComponent || components.length() > 0);
    }
    
    @Test
    public void testGenerateComponentsForOtherGender() {
        JSONObject result = TestOrderComponentService.generateInsertStatementsWithAge(
            102, 2, "CBC", 1, "2000-01-01", "Other"
        );
        
        JSONArray components = result.getJSONArray("insertedComponents");
        
        // Verify that only 'All' gender components are included
        for (int i = 0; i < components.length(); i++) {
            String gender = components.getJSONObject(i).getString("gender");
            assertEquals("All", gender, "Other gender should only have 'All' components");
        }
    }
}
```

---

## Performance Considerations

### Optimization Tips
1. **Cache Panel Data**: Pre-load active panels to avoid repeated queries
2. **Batch Processing**: Use batch inserts for multiple test orders
3. **Index on panel_id, age_range, gender**: Add database indexes for faster queries
4. **Connection Pooling**: Use HikariCP or similar for connection management

### Query Optimization
```sql
-- Add indexes for better performance
CREATE INDEX idx_components_panel_age_gender 
ON components(panel_id, age_range, gender, status);

CREATE INDEX idx_test_order_component_test_order 
ON test_order_component(test_order_id);

CREATE INDEX idx_components_status 
ON components(status);
```

---

## Logging & Monitoring

```java
// Enable detailed logging
Logger logger = AppLogger.getLogger();

logger.info("Starting component generation for test_order_id: " + testOrderId);
logger.info("Patient age: " + patientAge + " years");
logger.info("Gender prioritization applied");
logger.info("Generated " + components.size() + " components");
logger.info("SQL transaction length: " + sqlTransaction.length() + " characters");
```
