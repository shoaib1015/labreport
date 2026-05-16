# Test Order Component SQL Examples

This file contains example SQL statements that would be generated for different test order scenarios.

## Example 1: Adult Male - CBC Panel

**Input Parameters:**
- test_order_id: 1
- patient_id: 101
- panel_name: CBC
- panel_id: 10
- age_group: Adult
- gender: Male

**Generated SQL INSERT Statements:**

```sql
INSERT INTO test_order_component 
(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) 
VALUES 
(1, 1, 'Hemoglobin', 'g/dL', '13.5-17.5 g/dL', NULL, 'Normal', '2026-05-14T10:30:45');

INSERT INTO test_order_component 
(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) 
VALUES 
(1, 2, 'WBC Count', 'cells/µL', '4500-11000 cells/µL', NULL, 'Normal', '2026-05-14T10:30:45');

INSERT INTO test_order_component 
(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) 
VALUES 
(1, 3, 'RBC Count', 'million/µL', '4.5-5.9 million/µL', NULL, 'Normal', '2026-05-14T10:30:45');

INSERT INTO test_order_component 
(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) 
VALUES 
(1, 4, 'Platelet Count', 'cells/µL', '150000-450000 cells/µL', NULL, 'Normal', '2026-05-14T10:30:45');

INSERT INTO test_order_component 
(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) 
VALUES 
(1, 5, 'Hematocrit', '%', '41-53%', NULL, 'Normal', '2026-05-14T10:30:45');
```

---

## Example 2: Adult Female - Lipid Profile

**Input Parameters:**
- test_order_id: 2
- patient_id: 102
- panel_name: Lipid Profile
- panel_id: 11
- age_group: Adult
- gender: Female

**Generated SQL INSERT Statements:**

```sql
INSERT INTO test_order_component 
(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) 
VALUES 
(2, 10, 'Total Cholesterol', 'mg/dL', '<200 mg/dL', NULL, 'Normal', '2026-05-14T10:35:22');

INSERT INTO test_order_component 
(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) 
VALUES 
(2, 11, 'LDL Cholesterol', 'mg/dL', '<100 mg/dL', NULL, 'Normal', '2026-05-14T10:35:22');

INSERT INTO test_order_component 
(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) 
VALUES 
(2, 12, 'HDL Cholesterol', 'mg/dL', '>40 mg/dL (Female)', NULL, 'Normal', '2026-05-14T10:35:22');

INSERT INTO test_order_component 
(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) 
VALUES 
(2, 13, 'Triglycerides', 'mg/dL', '<150 mg/dL', NULL, 'Normal', '2026-05-14T10:35:22');

INSERT INTO test_order_component 
(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) 
VALUES 
(2, 14, 'VLDL Cholesterol', 'mg/dL', '<40 mg/dL', NULL, 'Normal', '2026-05-14T10:35:22');
```

---

## Example 3: Child (Other Gender) - Liver Function Test

**Input Parameters:**
- test_order_id: 3
- patient_id: 103
- panel_name: Liver Function Test
- panel_id: 12
- age_group: Child
- gender: Other

**Generated SQL INSERT Statements:**

```sql
INSERT INTO test_order_component 
(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) 
VALUES 
(3, 20, 'Total Bilirubin', 'mg/dL', '0.1-1.2 mg/dL (Child)', NULL, 'Normal', '2026-05-14T10:40:15');

INSERT INTO test_order_component 
(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) 
VALUES 
(3, 21, 'Direct Bilirubin', 'mg/dL', '0.0-0.3 mg/dL (Child)', NULL, 'Normal', '2026-05-14T10:40:15');

INSERT INTO test_order_component 
(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) 
VALUES 
(3, 22, 'AST (SGOT)', 'U/L', '10-40 U/L (Child)', NULL, 'Normal', '2026-05-14T10:40:15');

INSERT INTO test_order_component 
(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) 
VALUES 
(3, 23, 'ALT (SGPT)', 'U/L', '7-56 U/L (Child)', NULL, 'Normal', '2026-05-14T10:40:15');

INSERT INTO test_order_component 
(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) 
VALUES 
(3, 24, 'Alkaline Phosphatase', 'U/L', '30-300 U/L (Child)', NULL, 'Normal', '2026-05-14T10:40:15');

INSERT INTO test_order_component 
(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) 
VALUES 
(3, 25, 'Total Protein', 'g/dL', '6.0-8.3 g/dL', NULL, 'Normal', '2026-05-14T10:40:15');

INSERT INTO test_order_component 
(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) 
VALUES 
(3, 26, 'Albumin', 'g/dL', '3.5-5.0 g/dL', NULL, 'Normal', '2026-05-14T10:40:15');

INSERT INTO test_order_component 
(test_order_id, component_id, component_name, unit, reference_range, result_value, flag, created_at) 
VALUES 
(3, 27, 'Globulin', 'g/dL', '2.0-3.5 g/dL', NULL, 'Normal', '2026-05-14T10:40:15');
```

---

## Database State After Insertions

### test_order Table
```
id | patient_id | panel_id | panel_name           | created_at
---|------------|----------|----------------------|------------------
1  | 101        | 10       | CBC                  | 2026-05-14...
2  | 102        | 11       | Lipid Profile        | 2026-05-14...
3  | 103        | 12       | Liver Function Test  | 2026-05-14...
```

### test_order_component Table (Sample Rows)
```
id | test_order_id | component_id | component_name | unit        | reference_range | result_value | flag   | created_at
---|---------------|--------------|----------------|-------------|-----------------|--------------|--------|------------------
1  | 1             | 1            | Hemoglobin     | g/dL        | 13.5-17.5 g/dL | NULL         | Normal | 2026-05-14...
2  | 1             | 2            | WBC Count      | cells/µL    | 4500-11000...   | NULL         | Normal | 2026-05-14...
3  | 1             | 3            | RBC Count      | million/µL  | 4.5-5.9...      | NULL         | Normal | 2026-05-14...
4  | 2             | 10           | Total Cholesterol | mg/dL    | <200 mg/dL      | NULL         | Normal | 2026-05-14...
5  | 2             | 11           | LDL Cholesterol | mg/dL     | <100 mg/dL      | NULL         | Normal | 2026-05-14...
...
```

---

## Query Examples

### Get all components for a test order
```sql
SELECT * FROM test_order_component WHERE test_order_id = 1;
```

### Get all components with test order details
```sql
SELECT 
    toc.id,
    toc.component_name,
    toc.unit,
    toc.reference_range,
    toc.result_value,
    toc.flag,
    to.panel_name,
    p.name as patient_name
FROM test_order_component toc
JOIN test_order to ON toc.test_order_id = to.id
JOIN patients p ON to.patient_id = p.id
WHERE toc.test_order_id = 1;
```

### Update component result
```sql
UPDATE test_order_component 
SET result_value = '14.5 g/dL', flag = 'Normal', updated_at = datetime('now') 
WHERE id = 1;
```

### Count components by panel
```sql
SELECT 
    to.panel_name,
    COUNT(toc.id) as component_count
FROM test_order_component toc
JOIN test_order to ON toc.test_order_id = to.id
GROUP BY to.panel_name;
```

---

## Important Notes

1. **Age-Based Normal Ranges:** Different age groups (Child/Adult) have different reference ranges
2. **Gender-Specific Ranges:** Some components have gender-specific normal ranges (e.g., HDL Cholesterol)
3. **NULL Results:** All result_value fields are initially NULL, filled when test results are entered
4. **Default Flag:** All components start with 'Normal' flag, updated based on results
5. **Timestamps:** Both created_at and updated_at are automatically managed

---

## Test Execution Flow

1. **Create Test Order**
   ```
   INSERT INTO test_order (patient_id, panel_id, panel_name, ...) VALUES (...)
   ```

2. **Insert Components** (using TestOrderComponentService)
   ```
   INSERT INTO test_order_component (test_order_id, component_id, ...) VALUES (...)
   FOR EACH component in selected panel
   ```

3. **Enter Results** (when test is performed)
   ```
   UPDATE test_order_component SET result_value = '...', flag = 'Normal/Abnormal' WHERE id = ?
   ```

4. **Generate Report** (when needed)
   ```
   SELECT * FROM test_order_component WHERE test_order_id = ? ORDER BY component_id
   ```
