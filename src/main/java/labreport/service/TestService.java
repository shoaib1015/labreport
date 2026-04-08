package labreport.service;

import labreport.model.Test;
import labreport.model.TestCategory;
import labreport.model.SubTest;
import labreport.db.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TestService {
    private DatabaseManager dbManager;

    public TestService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // ============ TEST CATEGORY METHODS ============

    /**
     * Get all test categories
     */
    public List<TestCategory> getAllCategories() {
        List<TestCategory> categories = new ArrayList<>();
        String sql = "SELECT * FROM test_categories WHERE status = 'Active' ORDER BY display_order ASC, category_name ASC";
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                categories.add(mapRowToTestCategory(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    /**
     * Get category by ID
     */
    public TestCategory getCategoryById(int categoryId) {
        String sql = "SELECT * FROM test_categories WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, categoryId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTestCategory(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Create a new test category
     */
    public int createCategory(TestCategory category) {
        String sql = "INSERT INTO test_categories (category_name, category_description, display_order, status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, category.getCategoryName());
            pstmt.setString(2, category.getCategoryDescription());
            pstmt.setInt(3, category.getDisplayOrder());
            pstmt.setString(4, category.getStatus());
            pstmt.setLong(5, category.getCreatedAt());
            pstmt.setLong(6, System.currentTimeMillis());
            
            if (pstmt.executeUpdate() > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Update a test category
     */
    public boolean updateCategory(TestCategory category) {
        String sql = "UPDATE test_categories SET category_name = ?, category_description = ?, " +
                     "display_order = ?, status = ?, updated_at = ? WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, category.getCategoryName());
            pstmt.setString(2, category.getCategoryDescription());
            pstmt.setInt(3, category.getDisplayOrder());
            pstmt.setString(4, category.getStatus());
            pstmt.setLong(5, System.currentTimeMillis());
            pstmt.setInt(6, category.getId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ============ TEST METHODS ============

    /**
     * Get all tests with their sub-tests
     */
    public List<Test> getAllTests() {
        List<Test> tests = new ArrayList<>();
        String sql = "SELECT DISTINCT t.*, tc.category_name, tc.category_description FROM tests t " +
                     "LEFT JOIN test_categories tc ON t.category_id = tc.id " +
                     "WHERE t.status = 'Active' ORDER BY t.test_name ASC";
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Test test = mapRowToTest(rs);
                // Load sub-tests for this test
                test.setSubTests(getSubTestsByTestId(conn, test.getId()));
                tests.add(test);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tests;
    }

    /**
     * Get test by ID with sub-tests
     */
    public Test getTestById(int testId) {
        String sql = "SELECT t.*, tc.category_name, tc.category_description FROM tests t " +
                     "LEFT JOIN test_categories tc ON t.category_id = tc.id " +
                     "WHERE t.id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, testId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Test test = mapRowToTest(rs);
                    // Load sub-tests
                    test.setSubTests(getSubTestsByTestId(conn, testId));
                    return test;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Search tests by name
     */
    public List<Test> searchTestsByName(String testName) {
        List<Test> tests = new ArrayList<>();
        String sql = "SELECT t.*, tc.category_name, tc.category_description FROM tests t " +
                     "LEFT JOIN test_categories tc ON t.category_id = tc.id " +
                     "WHERE t.test_name LIKE ? AND t.status = 'Active' ORDER BY t.test_name ASC";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + testName + "%");
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Test test = mapRowToTest(rs);
                    test.setSubTests(getSubTestsByTestId(conn, test.getId()));
                    tests.add(test);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tests;
    }

    /**
     * Create a new test
     */
    public int createTest(Test test) {
        String sql = "INSERT INTO tests (test_name, base_price, unit, description, category_id, " +
                     "bold_format, border_format, highlight_format, commission_percentage, status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, test.getTestName());
            pstmt.setObject(2, test.getBasePrice());
            pstmt.setString(3, test.getUnit());
            pstmt.setString(4, test.getDescription());
            pstmt.setObject(5, test.getCategoryId()); // Null-safe
            pstmt.setBoolean(6, test.isBoldFormat());
            pstmt.setBoolean(7, test.isBorderFormat());
            pstmt.setBoolean(8, test.isHighlightFormat());
            if (test.getCommissionPercentage() != null) {
                pstmt.setDouble(9, test.getCommissionPercentage());
            } else {
                pstmt.setNull(9, Types.REAL);
            }
            pstmt.setString(10, test.getStatus());
            pstmt.setLong(11, test.getCreatedAt());
            pstmt.setLong(12, System.currentTimeMillis());
            
            if (pstmt.executeUpdate() > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Update a test
     */
    public boolean updateTest(Test test) {
        String sql = "UPDATE tests SET test_name = ?, base_price = ?, unit = ?, description = ?, " +
                     "category_id = ?, bold_format = ?, border_format = ?, highlight_format = ?, " +
                     "commission_percentage = ?, status = ?, updated_at = ? WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, test.getTestName());
            pstmt.setObject(2, test.getBasePrice());
            pstmt.setString(3, test.getUnit());
            pstmt.setString(4, test.getDescription());
            pstmt.setObject(5, test.getCategoryId());
            pstmt.setBoolean(6, test.isBoldFormat());
            pstmt.setBoolean(7, test.isBorderFormat());
            pstmt.setBoolean(8, test.isHighlightFormat());
            if (test.getCommissionPercentage() != null) {
                pstmt.setDouble(9, test.getCommissionPercentage());
            } else {
                pstmt.setNull(9, Types.REAL);
            }
            pstmt.setString(10, test.getStatus());
            pstmt.setLong(11, System.currentTimeMillis());
            pstmt.setInt(12, test.getId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Delete a test (soft delete)
     */
    public boolean deleteTest(int testId) {
        String sql = "UPDATE tests SET status = 'Inactive', updated_at = ? WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis());
            pstmt.setInt(2, testId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ============ SUB-TEST METHODS ============

    /**
     * Get all sub-tests for a test
     */
    public List<SubTest> getSubTestsByTestId(int testId) {
        try (Connection conn = dbManager.getConnection()) {
            return getSubTestsByTestId(conn, testId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * Get all sub-tests for a test (with connection)
     */
    private List<SubTest> getSubTestsByTestId(Connection conn, int testId) {
        List<SubTest> subTests = new ArrayList<>();
        String sql = "SELECT * FROM sub_tests WHERE test_id = ? ORDER BY id ASC";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, testId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    subTests.add(mapRowToSubTest(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return subTests;
    }

    /**
     * Create a sub-test
     */
    public int createSubTest(SubTest subTest) {
        String sql = "INSERT INTO sub_tests (test_id, sub_test_name, unit, normal_range_min, " +
                     "normal_range_max, age_ranges, price, instructions, print_instructions, display_order, status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, subTest.getTestId());
            pstmt.setString(2, subTest.getSubTestName());
            pstmt.setString(3, subTest.getUnit());
            pstmt.setObject(4, subTest.getNormalRangeMin());
            pstmt.setObject(5, subTest.getNormalRangeMax());
            pstmt.setString(6, subTest.getAgeRanges());
            pstmt.setObject(7, subTest.getPrice());
            pstmt.setString(8, subTest.getInstructions());
            pstmt.setBoolean(9, subTest.isPrintInstructions());
            pstmt.setInt(10, subTest.getDisplayOrder());
            pstmt.setString(11, subTest.getStatus());
            pstmt.setLong(12, subTest.getCreatedAt());
            pstmt.setLong(13, System.currentTimeMillis());
            
            if (pstmt.executeUpdate() > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Update a sub-test
     */
    public boolean updateSubTest(SubTest subTest) {
        String sql = "UPDATE sub_tests SET sub_test_name = ?, unit = ?, normal_range_min = ?, " +
                     "normal_range_max = ?, age_ranges = ?, price = ?, instructions = ?, print_instructions = ?, display_order = ?, status = ?, updated_at = ? WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, subTest.getSubTestName());
            pstmt.setString(2, subTest.getUnit());
            pstmt.setObject(3, subTest.getNormalRangeMin());
            pstmt.setObject(4, subTest.getNormalRangeMax());
            pstmt.setString(5, subTest.getAgeRanges());
            pstmt.setObject(6, subTest.getPrice());
            pstmt.setString(7, subTest.getInstructions());
            pstmt.setBoolean(8, subTest.isPrintInstructions());
            pstmt.setInt(9, subTest.getDisplayOrder());
            pstmt.setString(10, subTest.getStatus());
            pstmt.setLong(11, System.currentTimeMillis());
            pstmt.setInt(12, subTest.getId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Delete a sub-test
     */
    public boolean deleteSubTest(int subTestId) {
        String sql = "DELETE FROM sub_tests WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, subTestId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ============ HELPER METHODS ============

    private TestCategory mapRowToTestCategory(ResultSet rs) throws SQLException {
        TestCategory category = new TestCategory();
        category.setId(rs.getInt("id"));
        category.setCategoryName(rs.getString("category_name"));
        category.setCategoryDescription(rs.getString("category_description"));
        category.setDisplayOrder(rs.getInt("display_order"));
        category.setStatus(rs.getString("status"));
        category.setCreatedAt(rs.getLong("created_at"));
        category.setUpdatedAt(rs.getLong("updated_at"));
        return category;
    }

    private Test mapRowToTest(ResultSet rs) throws SQLException {
        Test test = new Test();
        test.setId(rs.getInt("id"));
        test.setTestName(rs.getString("test_name"));
        test.setBasePrice(rs.getObject("base_price") != null ? rs.getDouble("base_price") : null);
        test.setUnit(rs.getString("unit"));
        test.setDescription(rs.getString("description"));
        test.setCategoryId(rs.getObject("category_id") != null ? rs.getInt("category_id") : null);
        Object commissionObj = rs.getObject("commission_percentage");
        test.setCommissionPercentage(commissionObj != null ? rs.getDouble("commission_percentage") : null);
        test.setBoldFormat(rs.getBoolean("bold_format"));
        test.setBorderFormat(rs.getBoolean("border_format"));
        test.setHighlightFormat(rs.getBoolean("highlight_format"));
        test.setStatus(rs.getString("status"));
        test.setCreatedAt(rs.getLong("created_at"));
        test.setUpdatedAt(rs.getLong("updated_at"));
        
        // Set category object if available
        String categoryName = rs.getString("category_name");
        if (categoryName != null) {
            TestCategory category = new TestCategory();
            category.setId(rs.getInt("category_id"));
            category.setCategoryName(categoryName);
            category.setCategoryDescription(rs.getString("category_description"));
            test.setCategory(category);
        }
        
        return test;
    }

    private SubTest mapRowToSubTest(ResultSet rs) throws SQLException {
        SubTest subTest = new SubTest();
        subTest.setId(rs.getInt("id"));
        subTest.setTestId(rs.getInt("test_id"));
        subTest.setSubTestName(rs.getString("sub_test_name"));
        subTest.setUnit(rs.getString("unit"));
        Object normalMin = rs.getObject("normal_range_min");
        subTest.setNormalRangeMin(normalMin != null ? rs.getDouble("normal_range_min") : null);
        Object normalMax = rs.getObject("normal_range_max");
        subTest.setNormalRangeMax(normalMax != null ? rs.getDouble("normal_range_max") : null);
        subTest.setAgeRanges(rs.getString("age_ranges"));
        Object priceObj = rs.getObject("price");
        subTest.setPrice(priceObj != null ? rs.getDouble("price") : null);
        subTest.setInstructions(rs.getString("instructions"));
        subTest.setPrintInstructions(rs.getBoolean("print_instructions"));
        subTest.setDisplayOrder(rs.getInt("display_order"));
        subTest.setStatus(rs.getString("status"));
        subTest.setCreatedAt(rs.getLong("created_at"));
        subTest.setUpdatedAt(rs.getLong("updated_at"));
        return subTest;
    }
}
