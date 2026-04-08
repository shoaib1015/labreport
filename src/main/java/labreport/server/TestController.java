package labreport.server;

import labreport.model.Test;
import labreport.model.TestCategory;
import labreport.model.SubTest;
import labreport.service.TestService;
import labreport.db.DatabaseManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * REST API Controller for Test Management
 * Endpoints:
 * - GET /api/tests - Get all tests
 * - GET /api/tests?search=name - Search tests by name
 * - GET /api/tests/{id} - Get test by ID with sub-tests
 * - POST /api/tests - Create test
 * - PUT /api/tests/{id} - Update test
 * - DELETE /api/tests/{id} - Delete test
 * - GET /api/tests/{id}/subtests - Get sub-tests for test
 * - POST /api/tests/{id}/subtests - Add sub-test
 * - PUT /api/subtests/{id} - Update sub-test
 * - DELETE /api/subtests/{id} - Delete sub-test
 * - GET /api/categories - Get all test categories
 * - POST /api/categories - Create category
 * - PUT /api/categories/{id} - Update category
 */
public class TestController implements HttpHandler {
    private TestService testService;

    public TestController(DatabaseManager dbManager) {
        this.testService = new TestService(dbManager);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();
        
        try {
            enableCors(exchange);
            
            if ("GET".equalsIgnoreCase(method)) {
                handleGet(exchange, path, query);
            } else if ("POST".equalsIgnoreCase(method)) {
                handlePost(exchange, path);
            } else if ("PUT".equalsIgnoreCase(method)) {
                handlePut(exchange, path);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                handleDelete(exchange, path);
            } else if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(200, -1);
            } else {
                sendErrorResponse(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Handle GET requests
     */
    private void handleGet(HttpExchange exchange, String path, String query) throws IOException {
        if (path.contains("/categories")) {
            handleGetCategories(exchange, path, query);
        } else if (path.contains("/subtests/")) {
            handleGetSubTest(exchange, path);
        } else if (path.contains("/subtests")) {
            handleGetSubTests(exchange, path);
        } else {
            handleGetTests(exchange, path, query);
        }
    }

    /**
     * Handle GET for tests
     */
    private void handleGetTests(HttpExchange exchange, String path, String query) throws IOException {
        String[] pathParts = path.split("/");
        
        if (pathParts.length > 3 && !pathParts[3].isEmpty() && !pathParts[3].equals("subtests")) {
            // GET /api/tests/{id}
            try {
                int testId = Integer.parseInt(pathParts[3]);
                Test test = testService.getTestById(testId);
                
                if (test != null) {
                    sendJsonResponse(exchange, 200, testToJson(test).toString());
                } else {
                    sendErrorResponse(exchange, 404, "Test not found");
                }
            } catch (NumberFormatException e) {
                sendErrorResponse(exchange, 400, "Invalid test ID");
            }
        } else if (query != null && query.contains("search=")) {
            // GET /api/tests?search=name
            String searchTerm = query.split("search=")[1];
            searchTerm = java.net.URLDecoder.decode(searchTerm, StandardCharsets.UTF_8.name());
            
            List<Test> tests = testService.searchTestsByName(searchTerm);
            sendJsonResponse(exchange, 200, testsToJsonArray(tests).toString());
        } else {
            // GET /api/tests - Get all tests
            List<Test> tests = testService.getAllTests();
            sendJsonResponse(exchange, 200, testsToJsonArray(tests).toString());
        }
    }

    /**
     * Handle GET for sub-tests of a test
     */
    private void handleGetSubTests(HttpExchange exchange, String path) throws IOException {
        // GET /api/tests/{id}/subtests
        String[] pathParts = path.split("/");
        
        try {
            if (pathParts.length > 3 && !pathParts[3].isEmpty()) {
                int testId = Integer.parseInt(pathParts[3]);
                List<SubTest> subTests = testService.getSubTestsByTestId(testId);
                sendJsonResponse(exchange, 200, subTestsToJsonArray(subTests).toString());
            } else {
                sendErrorResponse(exchange, 400, "Invalid test ID");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid test ID");
        }
    }

    /**
     * Handle GET for a single sub-test
     */
    private void handleGetSubTest(HttpExchange exchange, String path) throws IOException {
        sendErrorResponse(exchange, 501, "Not implemented");
    }

    /**
     * Handle GET for categories
     */
    private void handleGetCategories(HttpExchange exchange, String path, String query) throws IOException {
        List<TestCategory> categories = testService.getAllCategories();
        sendJsonResponse(exchange, 200, categoriesToJsonArray(categories).toString());
    }

    /**
     * Handle POST requests
     */
    private void handlePost(HttpExchange exchange, String path) throws IOException {
        try {
            String requestBody = readRequestBody(exchange);
            JSONObject json = new JSONObject(requestBody);
            
            if (path.contains("/subtests")) {
                // POST /api/tests/{id}/subtests
                String[] pathParts = path.split("/");
                int testId = Integer.parseInt(pathParts[3]);
                
                SubTest subTest = jsonToSubTest(json);
                subTest.setTestId(testId);
                
                int subTestId = testService.createSubTest(subTest);
                if (subTestId > 0) {
                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Sub-test created successfully");
                    response.put("id", subTestId);
                    sendJsonResponse(exchange, 201, response.toString());
                } else {
                    sendErrorResponse(exchange, 500, "Failed to create sub-test");
                }
            } else if (path.contains("/categories")) {
                // POST /api/categories
                TestCategory category = jsonToCategory(json);
                int categoryId = testService.createCategory(category);
                
                if (categoryId > 0) {
                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Category created successfully");
                    response.put("id", categoryId);
                    sendJsonResponse(exchange, 201, response.toString());
                } else {
                    sendErrorResponse(exchange, 500, "Failed to create category");
                }
            } else {
                // POST /api/tests
                Test test = jsonToTest(json);
                int testId = testService.createTest(test);
                
                if (testId > 0) {
                    test.setId(testId);
                    if (json.has("subTests")) {
                        JSONArray subTestsArray = json.getJSONArray("subTests");
                        for (int i = 0; i < subTestsArray.length(); i++) {
                            SubTest subTest = jsonToSubTest(subTestsArray.getJSONObject(i));
                            subTest.setTestId(testId);
                            testService.createSubTest(subTest);
                        }
                    }
                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Test created successfully");
                    response.put("id", testId);
                    sendJsonResponse(exchange, 201, response.toString());
                } else {
                    sendErrorResponse(exchange, 500, "Failed to create test");
                }
            }
        } catch (JSONException e) {
            sendErrorResponse(exchange, 400, "Invalid JSON: " + e.getMessage());
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid ID format");
        }
    }

    /**
     * Handle PUT requests
     */
    private void handlePut(HttpExchange exchange, String path) throws IOException {
        try {
            String requestBody = readRequestBody(exchange);
            JSONObject json = new JSONObject(requestBody);
            String[] pathParts = path.split("/");
            
            if (path.contains("/subtests/")) {
                // PUT /api/subtests/{id}
                int subTestId = Integer.parseInt(pathParts[3]);
                SubTest subTest = jsonToSubTest(json);
                subTest.setId(subTestId);
                
                if (testService.updateSubTest(subTest)) {
                    sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Sub-test updated successfully\"}");
                } else {
                    sendErrorResponse(exchange, 500, "Failed to update sub-test");
                }
            } else if (path.contains("/categories/")) {
                // PUT /api/categories/{id}
                int categoryId = Integer.parseInt(pathParts[3]);
                TestCategory category = jsonToCategory(json);
                category.setId(categoryId);
                
                if (testService.updateCategory(category)) {
                    sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Category updated successfully\"}");
                } else {
                    sendErrorResponse(exchange, 500, "Failed to update category");
                }
            } else {
                // PUT /api/tests/{id}
                int testId = Integer.parseInt(pathParts[3]);
                Test test = jsonToTest(json);
                test.setId(testId);
                
                if (testService.updateTest(test)) {
                    if (json.has("subTests")) {
                        JSONArray subTestsArray = json.getJSONArray("subTests");
                        for (int i = 0; i < subTestsArray.length(); i++) {
                            JSONObject subTestJson = subTestsArray.getJSONObject(i);
                            SubTest subTest = jsonToSubTest(subTestJson);
                            subTest.setTestId(testId);
                            if (subTestJson.has("id") && subTestJson.getInt("id") > 0) {
                                subTest.setId(subTestJson.getInt("id"));
                                testService.updateSubTest(subTest);
                            } else {
                                testService.createSubTest(subTest);
                            }
                        }
                    }
                    sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Test updated successfully\"}");
                } else {
                    sendErrorResponse(exchange, 500, "Failed to update test");
                }
            }
        } catch (JSONException e) {
            sendErrorResponse(exchange, 400, "Invalid JSON: " + e.getMessage());
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid ID format");
        }
    }

    /**
     * Handle DELETE requests
     */
    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        try {
            String[] pathParts = path.split("/");
            
            if (path.contains("/subtests/")) {
                // DELETE /api/subtests/{id}
                int subTestId = Integer.parseInt(pathParts[3]);
                
                if (testService.deleteSubTest(subTestId)) {
                    sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Sub-test deleted successfully\"}");
                } else {
                    sendErrorResponse(exchange, 500, "Failed to delete sub-test");
                }
            } else {
                // DELETE /api/tests/{id}
                int testId = Integer.parseInt(pathParts[3]);
                
                if (testService.deleteTest(testId)) {
                    sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Test deleted successfully\"}");
                } else {
                    sendErrorResponse(exchange, 500, "Failed to delete test");
                }
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid ID format");
        }
    }

    // ============ JSON CONVERSION HELPERS ============

    private JSONObject testToJson(Test test) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", test.getId());
        json.put("testName", test.getTestName());
        json.put("basePrice", test.getBasePrice());
        json.put("unit", test.getUnit());
        json.put("description", test.getDescription());
        json.put("categoryId", test.getCategoryId());
        json.put("commissionPercentage", test.getCommissionPercentage());
        if (test.getCategory() != null) {
            json.put("category", categoryToJson(test.getCategory()));
        }
        json.put("boldFormat", test.isBoldFormat());
        json.put("borderFormat", test.isBorderFormat());
        json.put("highlightFormat", test.isHighlightFormat());
        json.put("status", test.getStatus());
        json.put("subTests", subTestsToJsonArray(test.getSubTests()));
        json.put("createdAt", test.getCreatedAt());
        json.put("updatedAt", test.getUpdatedAt());
        return json;
    }

    private Test jsonToTest(JSONObject json) throws JSONException {
        Test test = new Test();
        if (json.has("testName") && !json.isNull("testName")) test.setTestName(json.getString("testName"));
        if (json.has("basePrice") && !json.isNull("basePrice")) test.setBasePrice(json.getDouble("basePrice"));
        if (json.has("unit") && !json.isNull("unit")) test.setUnit(json.getString("unit"));
        if (json.has("description") && !json.isNull("description")) test.setDescription(json.getString("description"));
        if (json.has("categoryId") && !json.isNull("categoryId")) test.setCategoryId(json.getInt("categoryId"));
        if (json.has("commissionPercentage") && !json.isNull("commissionPercentage")) test.setCommissionPercentage(json.getDouble("commissionPercentage"));
        if (json.has("boldFormat") && !json.isNull("boldFormat")) test.setBoldFormat(json.getBoolean("boldFormat"));
        if (json.has("borderFormat") && !json.isNull("borderFormat")) test.setBorderFormat(json.getBoolean("borderFormat"));
        if (json.has("highlightFormat") && !json.isNull("highlightFormat")) test.setHighlightFormat(json.getBoolean("highlightFormat"));
        if (json.has("status") && !json.isNull("status")) test.setStatus(json.getString("status"));
        return test;
    }

    private JSONObject subTestToJson(SubTest subTest) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", subTest.getId());
        json.put("testId", subTest.getTestId());
        json.put("subTestName", subTest.getSubTestName());
        json.put("unit", subTest.getUnit());
        json.put("normalRangeMin", subTest.getNormalRangeMin());
        json.put("normalRangeMax", subTest.getNormalRangeMax());
        json.put("ageRanges", subTest.getAgeRanges());
        json.put("price", subTest.getPrice());
        json.put("instructions", subTest.getInstructions());
        json.put("printInstructions", subTest.isPrintInstructions());
        json.put("displayOrder", subTest.getDisplayOrder());
        json.put("status", subTest.getStatus());
        json.put("createdAt", subTest.getCreatedAt());
        json.put("updatedAt", subTest.getUpdatedAt());
        return json;
    }

    private SubTest jsonToSubTest(JSONObject json) throws JSONException {
        SubTest subTest = new SubTest();
        if (json.has("subTestName") && !json.isNull("subTestName")) subTest.setSubTestName(json.getString("subTestName"));
        if (json.has("unit") && !json.isNull("unit")) subTest.setUnit(json.getString("unit"));
        if (json.has("normalRangeMin") && !json.isNull("normalRangeMin")) subTest.setNormalRangeMin(json.getDouble("normalRangeMin"));
        if (json.has("normalRangeMax") && !json.isNull("normalRangeMax")) subTest.setNormalRangeMax(json.getDouble("normalRangeMax"));
        if (json.has("ageRanges") && !json.isNull("ageRanges")) subTest.setAgeRanges(json.getString("ageRanges"));
        if (json.has("price") && !json.isNull("price")) subTest.setPrice(json.getDouble("price"));
        if (json.has("instructions") && !json.isNull("instructions")) subTest.setInstructions(json.getString("instructions"));
        if (json.has("printInstructions") && !json.isNull("printInstructions")) subTest.setPrintInstructions(json.getBoolean("printInstructions"));
        if (json.has("displayOrder") && !json.isNull("displayOrder")) subTest.setDisplayOrder(json.getInt("displayOrder"));
        if (json.has("status") && !json.isNull("status")) subTest.setStatus(json.getString("status"));
        return subTest;
    }

    private JSONObject categoryToJson(TestCategory category) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", category.getId());
        json.put("categoryName", category.getCategoryName());
        json.put("categoryDescription", category.getCategoryDescription());
        json.put("displayOrder", category.getDisplayOrder());
        json.put("status", category.getStatus());
        json.put("createdAt", category.getCreatedAt());
        json.put("updatedAt", category.getUpdatedAt());
        return json;
    }

    private TestCategory jsonToCategory(JSONObject json) throws JSONException {
        TestCategory category = new TestCategory();
        if (json.has("categoryName")) category.setCategoryName(json.getString("categoryName"));
        if (json.has("categoryDescription")) category.setCategoryDescription(json.getString("categoryDescription"));
        if (json.has("displayOrder")) category.setDisplayOrder(json.getInt("displayOrder"));
        if (json.has("status")) category.setStatus(json.getString("status"));
        return category;
    }

    private JSONArray testsToJsonArray(List<Test> tests) throws JSONException {
        JSONArray array = new JSONArray();
        for (Test test : tests) {
            array.put(testToJson(test));
        }
        return array;
    }

    private JSONArray subTestsToJsonArray(List<SubTest> subTests) throws JSONException {
        JSONArray array = new JSONArray();
        for (SubTest subTest : subTests) {
            array.put(subTestToJson(subTest));
        }
        return array;
    }

    private JSONArray categoriesToJsonArray(List<TestCategory> categories) throws JSONException {
        JSONArray array = new JSONArray();
        for (TestCategory category : categories) {
            array.put(categoryToJson(category));
        }
        return array;
    }

    /**
     * Read request body
     */
    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return new String(result.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Send JSON response
     */
    private void sendJsonResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBody.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBody.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Send error response
     */
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String errorMessage) throws IOException {
        JSONObject errorJson = new JSONObject();
        errorJson.put("error", errorMessage);
        String responseBody = errorJson.toString();
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, responseBody.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBody.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Enable CORS headers
     */
    private void enableCors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }
}
