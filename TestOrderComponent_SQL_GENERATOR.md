# Test Order Component SQL Generator

## Overview
The `TestOrderComponentService` class now includes a new method `generateInsertStatementsWithAge()` that generates SQL INSERT statements for the `test_order_component` table with intelligent age and gender-based component selection.

## Key Features

### 1. **Age Calculation from DOB**
- Automatically calculates patient age in years from date of birth
- Uses Java 8+ `LocalDate` and `Period` classes
- Formula: `Period.between(DOB, today).getYears()`

### 2. **Gender-Based Prioritization**
The algorithm applies the following prioritization rules:

| Patient Gender | Priority Order |
|---|---|
| Male | Male components > All-gender components |
| Female | Female components > All-gender components |
| Other | Only All-gender components |

**Example:**
- If panel has Hemoglobin (Male) and Hemoglobin (All), a male patient gets the Male version
- If panel has only Hemoglobin (All), all patients get it
- A patient with gender='Other' only receives All-gender components

### 3. **Age Range Matching**
Components are stored with age ranges like "0-18", "19-65", "66-100"

**Matching Logic:**
```
1. Query components where: age_range_min <= patient_age <= age_range_max
2. Apply gender prioritization
3. If NO matches found, FALLBACK to age_range = "1-100"
```

**Age Range Format:** `"min-max"` (e.g., "0-18", "19-65")

### 4. **JSON Output Format**
Returns a JSON object with two keys:

```json
{
  "sqlTransaction": "BEGIN;\n  INSERT INTO ...\nCOMMIT;",
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

## Method Signature

```java
public static JSONObject generateInsertStatementsWithAge(
    int testOrderId,      // The test order ID
    int patientId,        // The patient ID
    String panelName,     // Panel name (e.g., 'CBC')
    int panelId,          // Panel ID from components table
    String patientDob,    // Patient DOB as 'YYYY-MM-DD'
    String gender         // Gender: 'Male', 'Female', or 'Other'
)
```

## Usage Example

### Java Code
```java
JSONObject result = TestOrderComponentService.generateInsertStatementsWithAge(
    101,              // test_order_id
    1,                // patient_id
    "CBC",            // panel_name
    1,                // panel_id
    "2021-05-10",     // patient_dob (DOB: May 10, 2021)
    "Male"            // gender
);

// Extract SQL transaction
String sqlTransaction = result.getString("sqlTransaction");

// Extract inserted components
JSONArray insertedComponents = result.getJSONArray("insertedComponents");

// Execute SQL (if using it)
// conn.createStatement().executeUpdate(sqlTransaction);
```

### Expected Input/Output

**Input:**
```json
{
  "test_order_id": 101,
  "patient_id": 1,
  "panel_name": "CBC",
  "panel_id": 1,
  "patient_dob": "2021-05-10",
  "gender": "Male"
}
```

**Output (for 5-year-old male patient, Age = 5):**
```json
{
  "sqlTransaction": "BEGIN;\nINSERT INTO test_order_component (test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at)\nVALUES (101, 1, 'Hemoglobin', 'g/dL', '11-14', NULL, 'Normal', '2026-05-28T...');\nINSERT INTO test_order_component (test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at)\nVALUES (101, 2, 'WBC Count', 'cells/µL', '5000-12000', NULL, 'Normal', '2026-05-28T...');\nCOMMIT;",
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
      "component_id": 2,
      "component_name": "WBC Count",
      "unit": "cells/µL",
      "reference_range": "5000-12000",
      "age_range": "0-18",
      "gender": "All"
    }
  ]
}
```

## Database Schema Requirements

The implementation assumes the following database schema:

### components table
```sql
CREATE TABLE components (
    component_id INTEGER PRIMARY KEY,
    panel_id INTEGER,
    component_name VARCHAR(100),
    unit VARCHAR(50),
    normal_range VARCHAR(50),      -- e.g., "11-14"
    age_range VARCHAR(20),          -- e.g., "0-18", "19-65", "1-100"
    gender VARCHAR(20),             -- 'Male', 'Female', or 'All'
    remarks TEXT,
    status VARCHAR(20),             -- 'Active' or 'Inactive'
    ...
);
```

### test_order_component table
```sql
CREATE TABLE test_order_component (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    test_order_id INTEGER,
    component_id INTEGER,
    component_name VARCHAR(100),
    unit VARCHAR(50),
    reference_range VARCHAR(50),
    result_value VARCHAR(100),
    flag VARCHAR(20),               -- 'Normal', 'Abnormal', etc.
    created_at TIMESTAMP,
    ...
);
```

## Algorithm Flow

```
1. Calculate Age
   age = Period.between(patientDob, today).getYears()

2. Query Components (First Pass - Exact Age Range)
   WHERE panel_id = ?
   AND status = 'Active'
   AND age_range != '1-100'
   AND isAgeInRange(patient_age, age_range)

3. Separate into Lists
   - gender_specific: components matching patient gender
   - gender_all: components with gender='All'

4. Apply Priority
   result = gender_specific + gender_all

5. If Result Empty, Fallback (Second Pass - All Ages)
   Query again with age_range = '1-100'
   Apply same prioritization

6. Build SQL Transaction
   FOR EACH component:
     INSERT INTO test_order_component (...)
       VALUES (...)

7. Build JSON Response
   Return {sqlTransaction, insertedComponents}
```

## Helper Methods

### `isAgeInRange(int patientAge, String ageRange): boolean`
Validates if patient age falls within age range string.
- Input: age=5, range="0-18" → Output: true
- Input: age=25, range="0-18" → Output: false

### `buildComponentMap(ResultSet rs): Map`
Extracts component data from database ResultSet into a Map.

### `getComponentsByAgeAndGender(int panelId, int age, String gender): List`
Retrieves and prioritizes components based on age and gender.

### `buildInsertStatementWithAge(int testOrderId, Map component): String`
Builds a single SQL INSERT statement with proper escaping for special characters.

## Dependencies

Added to `TestOrderComponentService.java`:
```java
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.Period;
```

These require the `json` (JSON-java) and Java 8+ libraries.

## Error Handling

- **Invalid DOB Format:** Throws `DateTimeParseException` if DOB is not 'YYYY-MM-DD'
- **Invalid Age Range:** Logs warning and skips range if format invalid
- **Database Errors:** Throws `RuntimeException` with detailed error message
- **Empty Results:** Logs warning but doesn't fail; returns empty components list

## Logging

All operations are logged using `AppLogger`:
```
INFO: Calculated patient age: 5 from DOB: 2021-05-10
INFO: Retrieved 5 components for panel_id: 1, age: 5, gender: Male
INFO: Generated SQL transaction with 5 components for test_order_id: 101
```

## SQL Injection Prevention

The implementation includes SQL escaping:
```java
private static String escapeSQL(String str) {
    if (str == null) return "";
    return str.replace("'", "''");  // Replace ' with ''
}
```

Special characters in component names, units, and ranges are escaped before inclusion in SQL.

## Performance Considerations

- **Database Queries:** Two queries max (exact range + fallback)
- **Age Calculation:** O(1) operation
- **Gender Prioritization:** O(n) where n = components for panel
- **String Building:** Linear in components count
- **Suitable for:** Real-time transaction processing

## Testing

Recommended test cases:
1. ✓ Child patient (age < 18) with Male gender
2. ✓ Adult patient (age >= 18) with Female gender
3. ✓ Exact age range match available
4. ✓ Fallback to "1-100" when no exact match
5. ✓ Other gender only gets 'All' components
6. ✓ Special characters in component names (SQL escaping)
7. ✓ Invalid DOB format (error handling)
8. ✓ Panel with no components (empty result)

## Future Enhancements

- Add caching for frequently accessed panels
- Support for complex age ranges (e.g., "0-5,10-18")
- Batch processing for multiple patients
- Custom component prioritization rules per hospital
- Result validation rules at component insertion
