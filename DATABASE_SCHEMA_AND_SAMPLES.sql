-- =====================================================
-- Database Schema for Test Order Component Generation
-- =====================================================

-- ===== COMPONENTS TABLE =====
-- Stores master list of lab test components with age/gender variations
CREATE TABLE components (
    component_id INTEGER PRIMARY KEY AUTOINCREMENT,
    panel_id INTEGER NOT NULL,
    component_name VARCHAR(100) NOT NULL,
    unit VARCHAR(50),
    normal_range VARCHAR(50),          -- Reference range (e.g., "11-14")
    age_range VARCHAR(20) NOT NULL,    -- Age range (e.g., "0-18", "19-65", "1-100")
    gender VARCHAR(20) NOT NULL,       -- 'Male', 'Female', or 'All'
    remarks TEXT,
    status VARCHAR(20) DEFAULT 'Active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (panel_id) REFERENCES panels(panel_id)
);

-- ===== TEST_ORDER_COMPONENT TABLE =====
-- Stores the result of component generation for each test order
CREATE TABLE test_order_component (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    test_order_id INTEGER NOT NULL,
    component_id INTEGER NOT NULL,
    component_name VARCHAR(100) NOT NULL,
    unit VARCHAR(50),
    reference_range VARCHAR(50),
    result_value VARCHAR(100),
    flag VARCHAR(20) DEFAULT 'Normal',  -- 'Normal', 'Abnormal', etc.
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (test_order_id) REFERENCES test_orders(test_order_id),
    FOREIGN KEY (component_id) REFERENCES components(component_id)
);

-- ===== SAMPLE DATA =====

-- Sample Panels
INSERT INTO panels (panel_id, panel_name, status) VALUES
    (1, 'CBC', 'Active'),
    (2, 'Lipid Profile', 'Active'),
    (3, 'Liver Function Test', 'Active'),
    (4, 'Kidney Function Test', 'Active'),
    (5, 'Thyroid Profile', 'Active');

-- ===== CBC (Complete Blood Count) COMPONENTS =====
-- Child-specific (0-18 years) - Male
INSERT INTO components VALUES 
    (1, 1, 'Hemoglobin', 'g/dL', '11.5-15.5', '0-18', 'Male', 'Red blood cell count for children', 'Active', CURRENT_TIMESTAMP);

-- Child-specific (0-18 years) - Female
INSERT INTO components VALUES 
    (2, 1, 'Hemoglobin', 'g/dL', '11-14', '0-18', 'Female', 'Red blood cell count for children', 'Active', CURRENT_TIMESTAMP);

-- Adult (19-65 years) - Male
INSERT INTO components VALUES 
    (3, 1, 'Hemoglobin', 'g/dL', '13.5-17.5', '19-65', 'Male', 'Red blood cell count for adults', 'Active', CURRENT_TIMESTAMP);

-- Adult (19-65 years) - Female
INSERT INTO components VALUES 
    (4, 1, 'Hemoglobin', 'g/dL', '12-15.5', '19-65', 'Female', 'Red blood cell count for adults', 'Active', CURRENT_TIMESTAMP);

-- Senior (66-100 years) - All genders
INSERT INTO components VALUES 
    (5, 1, 'Hemoglobin', 'g/dL', '12-16', '66-100', 'All', 'Red blood cell count for seniors', 'Active', CURRENT_TIMESTAMP);

-- All ages - Male
INSERT INTO components VALUES 
    (6, 1, 'WBC Count', 'cells/µL', '4500-11000', '0-18', 'All', 'White blood cell count (children)', 'Active', CURRENT_TIMESTAMP);

-- All ages - Female (no gender-specific needed)
INSERT INTO components VALUES 
    (7, 1, 'WBC Count', 'cells/µL', '4500-11000', '19-65', 'All', 'White blood cell count (adult)', 'Active', CURRENT_TIMESTAMP);

-- Platelet count - all ages
INSERT INTO components VALUES 
    (8, 1, 'Platelet Count', 'cells/µL', '150000-400000', '1-100', 'All', 'Platelet count for all ages', 'Active', CURRENT_TIMESTAMP);

-- ===== LIPID PROFILE COMPONENTS =====
-- Cholesterol - Adult Male
INSERT INTO components VALUES 
    (9, 2, 'Total Cholesterol', 'mg/dL', '<200', '19-65', 'Male', 'Total serum cholesterol', 'Active', CURRENT_TIMESTAMP);

-- Cholesterol - Adult Female
INSERT INTO components VALUES 
    (10, 2, 'Total Cholesterol', 'mg/dL', '<200', '19-65', 'Female', 'Total serum cholesterol', 'Active', CURRENT_TIMESTAMP);

-- Cholesterol - All ages fallback
INSERT INTO components VALUES 
    (11, 2, 'Total Cholesterol', 'mg/dL', '<200', '1-100', 'All', 'Total serum cholesterol (fallback)', 'Active', CURRENT_TIMESTAMP);

-- LDL - Adult
INSERT INTO components VALUES 
    (12, 2, 'LDL Cholesterol', 'mg/dL', '<100', '19-65', 'All', 'Low-density lipoprotein', 'Active', CURRENT_TIMESTAMP);

-- HDL - Male
INSERT INTO components VALUES 
    (13, 2, 'HDL Cholesterol', 'mg/dL', '>40', '19-65', 'Male', 'High-density lipoprotein (male)', 'Active', CURRENT_TIMESTAMP);

-- HDL - Female
INSERT INTO components VALUES 
    (14, 2, 'HDL Cholesterol', 'mg/dL', '>50', '19-65', 'Female', 'High-density lipoprotein (female)', 'Active', CURRENT_TIMESTAMP);

-- Triglycerides - All ages
INSERT INTO components VALUES 
    (15, 2, 'Triglycerides', 'mg/dL', '<150', '1-100', 'All', 'Fasting triglycerides', 'Active', CURRENT_TIMESTAMP);

-- ===== LIVER FUNCTION TEST COMPONENTS =====
-- ALT - Child
INSERT INTO components VALUES 
    (16, 3, 'ALT (SGPT)', 'U/L', '7-56', '0-18', 'All', 'Alanine aminotransferase (child)', 'Active', CURRENT_TIMESTAMP);

-- ALT - Adult
INSERT INTO components VALUES 
    (17, 3, 'ALT (SGPT)', 'U/L', '7-56', '19-65', 'All', 'Alanine aminotransferase (adult)', 'Active', CURRENT_TIMESTAMP);

-- AST - All ages
INSERT INTO components VALUES 
    (18, 3, 'AST (SGOT)', 'U/L', '10-40', '1-100', 'All', 'Aspartate aminotransferase', 'Active', CURRENT_TIMESTAMP);

-- Bilirubin - All ages
INSERT INTO components VALUES 
    (19, 3, 'Total Bilirubin', 'mg/dL', '0.1-1.2', '1-100', 'All', 'Total bilirubin', 'Active', CURRENT_TIMESTAMP);

-- Alkaline Phosphatase - All ages
INSERT INTO components VALUES 
    (20, 3, 'Alkaline Phosphatase', 'U/L', '30-120', '1-100', 'All', 'ALP enzyme', 'Active', CURRENT_TIMESTAMP);

-- ===== KIDNEY FUNCTION TEST COMPONENTS =====
-- Creatinine - Male
INSERT INTO components VALUES 
    (21, 4, 'Serum Creatinine', 'mg/dL', '0.7-1.3', '19-65', 'Male', 'Kidney function (male)', 'Active', CURRENT_TIMESTAMP);

-- Creatinine - Female
INSERT INTO components VALUES 
    (22, 4, 'Serum Creatinine', 'mg/dL', '0.6-1.2', '19-65', 'Female', 'Kidney function (female)', 'Active', CURRENT_TIMESTAMP);

-- BUN - All ages
INSERT INTO components VALUES 
    (23, 4, 'BUN', 'mg/dL', '7-20', '1-100', 'All', 'Blood urea nitrogen', 'Active', CURRENT_TIMESTAMP);

-- ===== THYROID PROFILE COMPONENTS =====
-- TSH - Male
INSERT INTO components VALUES 
    (24, 5, 'TSH', 'mIU/L', '0.4-4.0', '19-65', 'Male', 'Thyroid stimulating hormone (male)', 'Active', CURRENT_TIMESTAMP);

-- TSH - Female
INSERT INTO components VALUES 
    (25, 5, 'TSH', 'mIU/L', '0.3-3.0', '19-65', 'Female', 'Thyroid stimulating hormone (female)', 'Active', CURRENT_TIMESTAMP);

-- T3 - All ages
INSERT INTO components VALUES 
    (26, 5, 'Free T3', 'pg/mL', '2.3-4.2', '1-100', 'All', 'Free triiodothyronine', 'Active', CURRENT_TIMESTAMP);

-- T4 - All ages
INSERT INTO components VALUES 
    (27, 5, 'Free T4', 'ng/dL', '0.9-1.7', '1-100', 'All', 'Free thyroxine', 'Active', CURRENT_TIMESTAMP);

-- ===== TEST DATA QUERIES =====

-- View all CBC components
SELECT * FROM components WHERE panel_id = 1;

-- View components for age range 0-18
SELECT * FROM components WHERE age_range = '0-18';

-- View gender-specific components
SELECT * FROM components WHERE gender IN ('Male', 'Female');

-- View all-gender components
SELECT * FROM components WHERE gender = 'All';

-- View components with fallback range
SELECT * FROM components WHERE age_range = '1-100';

-- ===== VERIFICATION QUERIES =====

-- Verify gender prioritization for CBC (panel_id=1)
-- For a 10-year-old male:
-- Should get: Hemoglobin (Male, 0-18), WBC (All, 0-18), Platelet (All, 1-100)
SELECT 
    component_id, 
    component_name, 
    age_range, 
    gender, 
    normal_range 
FROM components 
WHERE panel_id = 1 
  AND (age_range = '0-18' OR age_range = '1-100')
  AND (gender = 'Male' OR gender = 'All')
ORDER BY gender DESC;  -- Male components first

-- Verify fallback to "1-100" for out-of-range ages
SELECT 
    component_id, 
    component_name, 
    age_range, 
    gender 
FROM components 
WHERE panel_id = 1 
  AND age_range = '1-100'
ORDER BY component_id;

-- Check for components that need "Other" gender (should only be 'All')
SELECT DISTINCT 
    panel_id, 
    component_name, 
    gender 
FROM components 
WHERE status = 'Active'
ORDER BY panel_id, gender;

-- ===== EXAMPLE OUTPUT =====
-- After executing generateInsertStatementsWithAge(101, 1, 'CBC', 1, '2021-05-10', 'Male')
-- Expected insertions:
-- | test_order_id | component_id | component_name | age_range | gender |
-- |---|---|---|---|---|
-- | 101 | 1 | Hemoglobin | 0-18 | Male |
-- | 101 | 6 | WBC Count | 0-18 | All |
-- | 101 | 8 | Platelet Count | 1-100 | All |
