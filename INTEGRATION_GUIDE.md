# Quick Integration Guide

This guide shows how to quickly integrate TestOrderComponentService with your existing handlers.

## 1. Basic Integration (3 Steps)

### Step 1: Import the Service
```java
import labreport.auth.TestOrderComponentService;
```

### Step 2: Call the Service in Your Handler
```java
public class PatientHandler extends AbstractHttpHandler {
    
    @Override
    protected void post(HttpExchange exchange, Map<String, String> params) throws Exception {
        try {
            // Get parameters from request
            int testOrderId = Integer.parseInt(params.get("test_order_id"));
            int patientId = Integer.parseInt(params.get("patient_id"));
            String panelName = params.get("panel_name");
            int panelId = Integer.parseInt(params.get("panel_id"));
            String ageGroup = params.get("age_group");      // "Child" or "Adult"
            String gender = params.get("gender");           // "Male", "Female", "Other"
            
            // Insert components
            int result = TestOrderComponentService.insertTestOrderComponents(
                testOrderId, patientId, panelName, panelId, ageGroup, gender
            );
            
            // Send success response
            sendJsonResponse(exchange, 200, 
                "{\"status\": \"success\", \"components_inserted\": " + result + "}");
                
        } catch (Exception e) {
            sendJsonResponse(exchange, 400, 
                "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
        }
    }
}
```

### Step 3: Call from Frontend (JavaScript)
```javascript
function createTestOrder() {
    const testOrder = {
        test_order_id: 1,
        patient_id: 101,
        panel_name: "CBC",
        panel_id: 10,
        age_group: "Adult",
        gender: "Male"
    };
    
    fetch('/api/test-orders', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(testOrder)
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            alert('Components created: ' + data.components_inserted);
        }
    });
}
```

---

## 2. Advanced: Component Result Entry

After components are created, allow users to enter results:

```java
// In PatientHandler.java
protected void updateComponentResult(HttpExchange exchange, Map<String, String> params) {
    try {
        int componentId = Integer.parseInt(params.get("component_id"));
        String resultValue = params.get("result_value");
        String flag = params.get("flag");  // "Normal" or "Abnormal"
        
        boolean success = TestOrderComponentService.updateComponentResult(
            componentId, resultValue, flag
        );
        
        sendJsonResponse(exchange, 200, 
            "{\"status\": \"" + (success ? "success" : "failed") + "\"}");
            
    } catch (Exception e) {
        sendJsonResponse(exchange, 400, 
            "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
    }
}
```

---

## 3. Display Components in Frontend

```javascript
function displayTestComponents(testOrderId) {
    fetch(`/api/test-orders/${testOrderId}/components`)
        .then(response => response.json())
        .then(components => {
            const tbody = document.getElementById('componentsTable');
            tbody.innerHTML = '';
            
            components.forEach(comp => {
                const row = tbody.insertRow();
                row.innerHTML = `
                    <td>${comp.component_name}</td>
                    <td>${comp.unit}</td>
                    <td>${comp.reference_range}</td>
                    <td><input type="text" class="result" placeholder="Enter result"></td>
                    <td><select class="flag">
                        <option>Normal</option>
                        <option>Abnormal</option>
                        <option>Critical</option>
                    </select></td>
                    <td><button onclick="saveResult(${comp.id}, this)">Save</button></td>
                `;
            });
        });
}

function saveResult(componentId, button) {
    const row = button.closest('tr');
    const result = row.querySelector('.result').value;
    const flag = row.querySelector('.flag').value;
    
    fetch('/api/components/result', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({
            component_id: componentId,
            result_value: result,
            flag: flag
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            alert('Result saved successfully');
        }
    });
}
```

---

## 4. Common Patterns

### Pattern 1: Automatic Age Group Detection
```java
private String detectAgeGroup(int patientId) throws Exception {
    // Get patient DOB from database
    String dob = PatientService.getPatientDOB(patientId);
    
    // Calculate age
    LocalDate birthDate = LocalDate.parse(dob);
    int age = Period.between(birthDate, LocalDate.now()).getYears();
    
    // Return age group
    return age < 18 ? "Child" : "Adult";
}
```

### Pattern 2: Validate Before Insertion
```java
private boolean validateTestOrder(Map<String, String> params) {
    List<String> requiredFields = Arrays.asList(
        "test_order_id", "patient_id", "panel_name", "panel_id", "age_group", "gender"
    );
    
    for (String field : requiredFields) {
        if (!params.containsKey(field) || params.get(field).isEmpty()) {
            return false;
        }
    }
    
    return true;
}
```

### Pattern 3: Log Components Created
```java
private void logComponentCreation(int testOrderId, int count) {
    AppLogger.getLogger().info(
        "Created test order #" + testOrderId + " with " + count + " components"
    );
}
```

---

## 5. API Endpoints Summary

| Endpoint | Method | Purpose | Example |
|----------|--------|---------|---------|
| `/api/test-orders` | POST | Create test order and components | See Step 2 |
| `/api/test-orders/{id}/components` | GET | Get all components for order | See Step 3 |
| `/api/components/result` | POST | Save component result | See Advanced |
| `/api/components/{id}` | GET | Get single component details | - |
| `/api/components/{id}` | PUT | Update component metadata | - |

---

## 6. Error Handling Checklist

- ✓ Validate all input parameters
- ✓ Check test_order exists before inserting components
- ✓ Handle invalid age_group values
- ✓ Handle invalid gender values
- ✓ Handle database connection errors
- ✓ Log all errors with context
- ✓ Return meaningful error messages to client

---

## 7. Testing Checklist

- ✓ Test with valid Adult Male CBC
- ✓ Test with valid Child Female Lipid Profile
- ✓ Test with valid Adult Other Liver Function Test
- ✓ Test with invalid panel name (should fail gracefully)
- ✓ Test with missing parameters
- ✓ Test result updates
- ✓ Verify database entries are correct

---

## 8. Production Deployment

Before deploying:
1. Run TestOrderComponentExample to verify functionality
2. Test with sample data
3. Verify database schema is created
4. Check logs for any warnings
5. Load test with multiple concurrent orders
6. Verify foreign key constraints work

---

## Quick Reference

**To insert test components:**
```java
TestOrderComponentService.insertTestOrderComponents(1, 101, "CBC", 10, "Adult", "Male");
```

**To get components for display:**
```java
List<Map<String, Object>> components = 
    TestOrderComponentService.getTestOrderComponents(1);
```

**To update a result:**
```java
TestOrderComponentService.updateComponentResult(1, "7.5 g/dL", "Normal");
```

**To generate SQL for audit:**
```java
List<String> statements = 
    TestOrderComponentService.generateInsertStatements(1, 101, "CBC", 10, "Adult", "Male");
```
