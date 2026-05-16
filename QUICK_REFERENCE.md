# Test Order Component Service - Quick Reference Card

## ⚡ 30-Second Overview

Generate SQL INSERT statements for test order components based on patient demographics.

```java
// One-line insertion
TestOrderComponentService.insertTestOrderComponents(
    1,              // test_order_id
    101,            // patient_id  
    "CBC",          // panel_name
    10,             // panel_id
    "Adult",        // age_group: "Child" or "Adult"
    "Male"          // gender: "Male", "Female", "Other"
);
```

---

## 🎯 Common Tasks

### 1. Insert Test Components
```java
int rows = TestOrderComponentService.insertTestOrderComponents(
    testOrderId, patientId, panelName, panelId, ageGroup, gender
);
// Returns: Number of components inserted
```

### 2. Get Components for Display
```java
List<Map<String, Object>> components = 
    TestOrderComponentService.getTestOrderComponents(testOrderId);

// Display:
for (Map<String, Object> c : components) {
    System.out.println(c.get("component_name") + " | " + 
                      c.get("reference_range") + " " + c.get("unit"));
}
```

### 3. Update Test Result
```java
TestOrderComponentService.updateComponentResult(
    componentId,      // From test_order_component.id
    "7.5 g/dL",      // Result value
    "Normal"         // Flag: Normal, Abnormal, Critical
);
```

### 4. Generate SQL for Logging
```java
List<String> sqlStatements = 
    TestOrderComponentService.generateInsertStatements(
        testOrderId, patientId, panelName, panelId, ageGroup, gender
    );
// Returns: SQL INSERT statements as strings
```

---

## 📋 Method Reference

| Method | Returns | Purpose |
|--------|---------|---------|
| `insertTestOrderComponents()` | int | Insert components directly |
| `generateInsertStatements()` | List<String> | Get SQL statements |
| `getTestOrderComponents()` | List<Map> | Retrieve components |
| `updateComponentResult()` | boolean | Update result/flag |

---

## 🔧 Integration Template

```java
import labreport.auth.TestOrderComponentService;

public class MyHandler {
    public void handleTestOrder(Map<String, String> params) {
        try {
            // Parse parameters
            int testOrderId = Integer.parseInt(params.get("test_order_id"));
            String panelName = params.get("panel_name");
            String ageGroup = params.get("age_group");
            String gender = params.get("gender");
            // ... more params
            
            // Insert components
            int result = TestOrderComponentService.insertTestOrderComponents(
                testOrderId, patientId, panelName, panelId, ageGroup, gender
            );
            
            // Respond
            sendJson(200, "{\"components\": " + result + "}");
        } catch (Exception e) {
            sendJson(400, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
```

---

## ✅ Valid Input Values

| Parameter | Valid Values | Example |
|-----------|--------------|---------|
| panel_name | 'CBC', 'Lipid Profile', 'Liver Function Test' | "CBC" |
| age_group | 'Child', 'Adult' | "Adult" |
| gender | 'Male', 'Female', 'Other' | "Male" |
| flag | 'Normal', 'Abnormal', 'Critical' | "Normal" |

---

## 📊 Database Tables

**test_order_component**
```
id | test_order_id | component_id | component_name | unit | reference_range | result_value | flag | created_at | updated_at
```

**Related Tables**
- `test_order`: Linked via test_order_id
- `components`: Matched via panel_name, ageRange, gender

---

## 🚨 Common Errors & Solutions

| Error | Cause | Solution |
|-------|-------|----------|
| NullPointerException | Missing parameter | Check all required params are provided |
| SQLException | Database error | Check test_order table has matching ID |
| No components inserted | Wrong panel_name | Verify panel_name in components table |
| Wrong components | Age/Gender mismatch | Check ageRange and gender in components |

---

## 💡 Tips & Tricks

### Tip 1: Auto-detect Age Group
```java
String ageGroup = age < 18 ? "Child" : "Adult";
```

### Tip 2: Log All Operations
```java
AppLogger.getLogger().info("Inserted " + result + " components for order " + testOrderId);
```

### Tip 3: Validate Before Inserting
```java
if (!isValidPanel(panelName)) {
    throw new IllegalArgumentException("Invalid panel: " + panelName);
}
```

### Tip 4: Batch Updates
```java
for (Map<String, Object> comp : components) {
    updateComponentResult((int)comp.get("id"), result, "Normal");
}
```

---

## 📁 Key Files

| File | Purpose |
|------|---------|
| TestOrderComponentService.java | Core service - use this |
| SchemaInitializer.java | Database setup - already updated |
| TestOrderComponentExample.java | Usage examples - reference this |
| INTEGRATION_GUIDE.md | Integration examples - read this |

---

## 🔗 Related Services

```java
// Get patient info
PatientService.getPatientDOB(patientId);

// Get test order details
// (implement in handler)

// Get panel components
// (use TestOrderComponentService)

// Update test results
TestOrderComponentService.updateComponentResult(...);
```

---

## 📱 Frontend Integration

```javascript
// Create test order and components
fetch('/api/test-orders', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify({
        test_order_id: 1,
        patient_id: 101,
        panel_name: 'CBC',
        panel_id: 10,
        age_group: 'Adult',
        gender: 'Male'
    })
});

// Get components for display
fetch(`/api/test-orders/${orderId}/components`)
    .then(r => r.json())
    .then(components => displayInTable(components));

// Submit result
fetch('/api/components/result', {
    method: 'POST',
    body: JSON.stringify({
        component_id: compId,
        result_value: '7.5 g/dL',
        flag: 'Normal'
    })
});
```

---

## 🧪 Quick Test

```java
// Test insertion
int result = TestOrderComponentService.insertTestOrderComponents(
    1, 101, "CBC", 10, "Adult", "Male"
);
System.out.println("Success: " + (result > 0)); // Should be true

// Test retrieval
List<Map<String, Object>> comp = 
    TestOrderComponentService.getTestOrderComponents(1);
System.out.println("Components: " + comp.size()); // Should be > 0

// Test update
boolean updated = TestOrderComponentService.updateComponentResult(1, "7.5", "Normal");
System.out.println("Updated: " + updated); // Should be true
```

---

## 📞 Support

- **Documentation**: See TestOrderComponentService_DOCUMENTATION.md
- **Examples**: See TestOrderComponentExample.java
- **SQL Reference**: See TestOrderComponent_SQL_EXAMPLES.md
- **Integration**: See INTEGRATION_GUIDE.md
- **Logs**: Check AppLogger output for details

---

## ⚙️ Configuration

### Database
- Type: SQLite
- File: data/labreport.db
- Table: test_order_component (auto-created)

### Logging
- Logger: AppLogger.getLogger()
- Level: INFO for success, SEVERE for errors
- Location: Check application logs

---

## 🎓 Learning Path

1. Read this Quick Reference (5 min)
2. Review TestOrderComponentExample.java (10 min)
3. Read TestOrderComponentService_DOCUMENTATION.md (15 min)
4. Check INTEGRATION_GUIDE.md (10 min)
5. Integrate into your handler (20 min)
6. Test with sample data (10 min)

**Total: ~70 minutes to full understanding**

---

**Last Updated:** May 14, 2026
**Version:** 1.0
**Status:** ✓ Production Ready
