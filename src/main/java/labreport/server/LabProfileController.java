package labreport.server;

import labreport.model.LabProfile;
import labreport.service.LabProfileService;
import labreport.db.DatabaseManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * REST API Controller for Lab Profile Management
 * Endpoints:
 * - GET /api/lab - Get lab profile
 * - POST /api/lab - Create or update lab profile
 * - PUT /api/lab - Update lab profile
 */
public class LabProfileController implements HttpHandler {
    private LabProfileService labProfileService;

    public LabProfileController(DatabaseManager dbManager) {
        this.labProfileService = new LabProfileService(dbManager);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        
        try {
            if ("GET".equalsIgnoreCase(method)) {
                handleGetLabProfile(exchange);
            } else if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                handleSaveLabProfile(exchange);
            } else {
                sendErrorResponse(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * GET /api/lab - Get lab profile
     */
    private void handleGetLabProfile(HttpExchange exchange) throws IOException {
        LabProfile profile = labProfileService.getLabProfile();
        
        if (profile != null) {
            JSONObject json = labProfileToJson(profile);
            sendJsonResponse(exchange, 200, json.toString());
        } else {
            sendJsonResponse(exchange, 200, "{}");
        }
    }

    /**
     * POST/PUT /api/lab - Create or update lab profile
     */
    private void handleSaveLabProfile(HttpExchange exchange) throws IOException {
        try {
            String requestBody = readRequestBody(exchange);
            JSONObject json = new JSONObject(requestBody);
            
            LabProfile profile = jsonToLabProfile(json);
            
            if (labProfileService.saveLabProfile(profile)) {
                sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Lab profile saved successfully\"}");
            } else {
                sendErrorResponse(exchange, 500, "Failed to save lab profile");
            }
        } catch (JSONException e) {
            sendErrorResponse(exchange, 400, "Invalid JSON: " + e.getMessage());
        }
    }

    /**
     * Convert LabProfile to JSON
     */
    private JSONObject labProfileToJson(LabProfile profile) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", profile.getId());
        json.put("labName", profile.getLabName());
        json.put("address", profile.getAddress());
        json.put("city", profile.getCity());
        json.put("state", profile.getState());
        json.put("postalCode", profile.getPostalCode());
        json.put("country", profile.getCountry());
        json.put("phones", profile.getPhones());
        json.put("email", profile.getEmail());
        json.put("website", profile.getWebsite());
        json.put("operatingHours", profile.getOperatingHours());
        json.put("ownerName", profile.getOwnerName());
        json.put("ownerPhone", profile.getOwnerPhone());
        json.put("ownerEmail", profile.getOwnerEmail());
        json.put("franchiseType", profile.getFranchiseType());
        json.put("reportHeader", profile.getReportHeader());
        json.put("reportFooter", profile.getReportFooter());
        json.put("reportDisclaimer", profile.getReportDisclaimer());
        json.put("gstNumber", profile.getGstNumber());
        json.put("gstRate", profile.getGstRate());
        json.put("enableGst", profile.isEnableGst());
        json.put("currency", profile.getCurrency());
        json.put("createdAt", profile.getCreatedAt());
        json.put("updatedAt", profile.getUpdatedAt());
        return json;
    }

    /**
     * Convert JSON to LabProfile
     */
    private LabProfile jsonToLabProfile(JSONObject json) throws JSONException {
        LabProfile profile = new LabProfile();
        
        if (json.has("id")) profile.setId(json.getInt("id"));
        if (json.has("labName")) profile.setLabName(json.getString("labName"));
        if (json.has("address")) profile.setAddress(json.getString("address"));
        if (json.has("city")) profile.setCity(json.getString("city"));
        if (json.has("state")) profile.setState(json.getString("state"));
        if (json.has("postalCode")) profile.setPostalCode(json.getString("postalCode"));
        if (json.has("country")) profile.setCountry(json.getString("country"));
        if (json.has("phones")) profile.setPhones(json.getString("phones"));
        if (json.has("email")) profile.setEmail(json.getString("email"));
        if (json.has("website")) profile.setWebsite(json.getString("website"));
        if (json.has("operatingHours")) profile.setOperatingHours(json.getString("operatingHours"));
        if (json.has("ownerName")) profile.setOwnerName(json.getString("ownerName"));
        if (json.has("ownerPhone")) profile.setOwnerPhone(json.getString("ownerPhone"));
        if (json.has("ownerEmail")) profile.setOwnerEmail(json.getString("ownerEmail"));
        if (json.has("franchiseType")) profile.setFranchiseType(json.getString("franchiseType"));
        if (json.has("reportHeader")) profile.setReportHeader(json.getString("reportHeader"));
        if (json.has("reportFooter")) profile.setReportFooter(json.getString("reportFooter"));
        if (json.has("reportDisclaimer")) profile.setReportDisclaimer(json.getString("reportDisclaimer"));
        if (json.has("gstNumber") && !json.isNull("gstNumber")) profile.setGstNumber(json.getString("gstNumber"));
        if (json.has("gstRate") && !json.isNull("gstRate")) profile.setGstRate(json.getDouble("gstRate"));
        if (json.has("enableGst") && !json.isNull("enableGst")) profile.setEnableGst(json.getBoolean("enableGst"));
        if (json.has("currency") && !json.isNull("currency")) profile.setCurrency(json.getString("currency"));
        
        return profile;
    }

    /**
     * Read request body from HttpExchange
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
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
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
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, responseBody.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBody.getBytes(StandardCharsets.UTF_8));
        }
    }
}
