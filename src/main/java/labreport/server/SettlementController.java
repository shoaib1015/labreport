package labreport.server;

import labreport.model.SettlementReport;
import labreport.model.DoctorCommission;
import labreport.service.SettlementService;
import labreport.service.LabProfileService;
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
 * REST API Controller for Settlement and Commission Management
 * Endpoints:
 * - GET /api/settlements - Get all settlement reports
 * - GET /api/settlements?doctorId=id - Get settlements for doctor
 * - GET /api/settlements?startDate=date&endDate=date - Get settlements by date range
 * - GET /api/settlements/{id} - Get settlement report by ID
 * - POST /api/settlements/calculate - Calculate settlement for doctor/date range
 * - POST /api/settlements - Create settlement record
 * - PUT /api/settlements/{id} - Update settlement
 * - DELETE /api/settlements/{id} - Delete settlement
 * - GET /api/commissions - Get all doctor commissions
 * - GET /api/commissions?doctorId=id - Get commissions for doctor
 * - POST /api/commissions - Create commission record
 * - PUT /api/commissions/{id} - Update commission
 * - DELETE /api/commissions/{id} - Delete commission
 */
public class SettlementController implements HttpHandler {
    private SettlementService settlementService;
    private LabProfileService labProfileService;

    public SettlementController(DatabaseManager dbManager) {
        this.settlementService = new SettlementService(dbManager);
        this.labProfileService = new LabProfileService(dbManager);
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
        if (path.contains("/commissions")) {
            handleGetCommissions(exchange, path, query);
        } else {
            handleGetSettlements(exchange, path, query);
        }
    }

    /**
     * Handle GET for settlements
     */
    private void handleGetSettlements(HttpExchange exchange, String path, String query) throws IOException {
        String[] pathParts = path.split("/");
        
        if (pathParts.length > 3 && !pathParts[3].isEmpty()) {
            // GET /api/settlements/{id}
            try {
                int settlementId = Integer.parseInt(pathParts[3]);
                SettlementReport settlement = settlementService.getSettlementById(settlementId);
                
                if (settlement != null) {
                    sendJsonResponse(exchange, 200, settlementToJson(settlement).toString());
                } else {
                    sendErrorResponse(exchange, 404, "Settlement not found");
                }
            } catch (NumberFormatException e) {
                sendErrorResponse(exchange, 400, "Invalid settlement ID");
            }
        } else if (query != null) {
            if (query.contains("doctorId=")) {
                // GET /api/settlements?doctorId=id
                try {
                    String doctorIdStr = query.split("doctorId=")[1];
                    int doctorId = Integer.parseInt(doctorIdStr);
                    
                    List<SettlementReport> settlements = settlementService.getSettlementsByDoctor(doctorId);
                    sendJsonResponse(exchange, 200, settlementsToJsonArray(settlements).toString());
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid doctor ID");
                }
            } else if (query.contains("startDate=")) {
                // GET /api/settlements?startDate=date&endDate=date
                try {
                    String[] parts = query.split("&");
                    String startDate = parts[0].split("startDate=")[1];
                    String endDate = parts.length > 1 ? parts[1].split("endDate=")[1] : startDate;
                    
                    startDate = java.net.URLDecoder.decode(startDate, StandardCharsets.UTF_8.name());
                    endDate = java.net.URLDecoder.decode(endDate, StandardCharsets.UTF_8.name());
                    
                    List<SettlementReport> settlements = settlementService.getSettlementsByDateRange(startDate, endDate);
                    sendJsonResponse(exchange, 200, settlementsToJsonArray(settlements).toString());
                } catch (Exception e) {
                    sendErrorResponse(exchange, 400, "Invalid date parameters");
                }
            } else {
                List<SettlementReport> settlements = settlementService.getAllSettlements();
                sendJsonResponse(exchange, 200, settlementsToJsonArray(settlements).toString());
            }
        } else {
            // GET /api/settlements - Get all settlements
            List<SettlementReport> settlements = settlementService.getAllSettlements();
            sendJsonResponse(exchange, 200, settlementsToJsonArray(settlements).toString());
        }
    }

    /**
     * Handle GET for commissions
     */
    private void handleGetCommissions(HttpExchange exchange, String path, String query) throws IOException {
        if (query != null && query.contains("doctorId=")) {
            // GET /api/commissions?doctorId=id
            try {
                String doctorIdStr = query.split("doctorId=")[1];
                int doctorId = Integer.parseInt(doctorIdStr);
                
                List<DoctorCommission> commissions = settlementService.getCommissionsByDoctor(doctorId);
                sendJsonResponse(exchange, 200, commissionsToJsonArray(commissions).toString());
            } catch (NumberFormatException e) {
                sendErrorResponse(exchange, 400, "Invalid doctor ID");
            }
        } else {
            // GET /api/commissions - Get all commissions
            List<DoctorCommission> commissions = settlementService.getAllCommissions();
            sendJsonResponse(exchange, 200, commissionsToJsonArray(commissions).toString());
        }
    }

    /**
     * Handle POST requests
     */
    private void handlePost(HttpExchange exchange, String path) throws IOException {
        try {
            String requestBody = readRequestBody(exchange);
            JSONObject json = new JSONObject(requestBody);
            
            if (path.contains("/calculate")) {
                // POST /api/settlements/calculate - Calculate settlement
                Integer doctorId = json.has("doctorId") ? json.getInt("doctorId") : null;
                String startDate = json.getString("startDate");
                String endDate = json.getString("endDate");
                
                Double gstRate = labProfileService.getLabProfile().getGstRate();
                SettlementReport settlement = settlementService.calculateSettlement(doctorId, startDate, endDate, gstRate, labProfileService);
                
                sendJsonResponse(exchange, 200, settlementToJson(settlement).toString());
            } else if (path.contains("/commissions")) {
                // POST /api/commissions - Create commission
                DoctorCommission commission = jsonToCommission(json);
                int commissionId = settlementService.createCommission(commission);
                
                if (commissionId > 0) {
                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Commission created successfully");
                    response.put("id", commissionId);
                    sendJsonResponse(exchange, 201, response.toString());
                } else {
                    sendErrorResponse(exchange, 500, "Failed to create commission");
                }
            } else {
                // POST /api/settlements - Create settlement record
                SettlementReport settlement = jsonToSettlement(json);
                int settlementId = settlementService.createSettlement(settlement);
                
                if (settlementId > 0) {
                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Settlement created successfully");
                    response.put("id", settlementId);
                    sendJsonResponse(exchange, 201, response.toString());
                } else {
                    sendErrorResponse(exchange, 500, "Failed to create settlement");
                }
            }
        } catch (JSONException e) {
            sendErrorResponse(exchange, 400, "Invalid JSON: " + e.getMessage());
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
            
            if (path.contains("/commissions/")) {
                // PUT /api/commissions/{id}
                int commissionId = Integer.parseInt(pathParts[3]);
                
                DoctorCommission commission = jsonToCommission(json);
                commission.setId(commissionId);
                
                if (settlementService.updateCommission(commission)) {
                    sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Commission updated successfully\"}");
                } else {
                    sendErrorResponse(exchange, 500, "Failed to update commission");
                }
            } else {
                // PUT /api/settlements/{id}
                int settlementId = Integer.parseInt(pathParts[3]);
                
                SettlementReport settlement = jsonToSettlement(json);
                settlement.setId(settlementId);
                
                if (settlementService.updateSettlement(settlement)) {
                    sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Settlement updated successfully\"}");
                } else {
                    sendErrorResponse(exchange, 500, "Failed to update settlement");
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
            
            if (path.contains("/commissions/")) {
                // DELETE /api/commissions/{id}
                int commissionId = Integer.parseInt(pathParts[3]);
                
                if (settlementService.deleteCommission(commissionId)) {
                    sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Commission deleted successfully\"}");
                } else {
                    sendErrorResponse(exchange, 500, "Failed to delete commission");
                }
            } else {
                // DELETE /api/settlements/{id}
                int settlementId = Integer.parseInt(pathParts[3]);
                
                if (settlementService.deleteSettlement(settlementId)) {
                    sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Settlement deleted successfully\"}");
                } else {
                    sendErrorResponse(exchange, 500, "Failed to delete settlement");
                }
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid ID format");
        }
    }

    // ============ JSON CONVERSION HELPERS ============

    private JSONObject settlementToJson(SettlementReport settlement) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", settlement.getId());
        json.put("doctorId", settlement.getDoctorId());
        json.put("startDate", settlement.getStartDate());
        json.put("endDate", settlement.getEndDate());
        json.put("totalReportsCount", settlement.getTotalReportsCount());
        json.put("subtotal", settlement.getSubtotal());
        json.put("gstAmount", settlement.getGstAmount());
        json.put("totalAmount", settlement.getTotalAmount());
        json.put("paymentStatus", settlement.getPaymentStatus());
        json.put("paymentDate", settlement.getPaymentDate());
        json.put("paymentMethod", settlement.getPaymentMethod());
        json.put("bankDetails", settlement.getBankDetails());
        json.put("notes", settlement.getNotes());
        json.put("createdAt", settlement.getCreatedAt());
        json.put("updatedAt", settlement.getUpdatedAt());
        return json;
    }

    private SettlementReport jsonToSettlement(JSONObject json) throws JSONException {
        SettlementReport settlement = new SettlementReport();
        if (json.has("doctorId")) settlement.setDoctorId(json.getInt("doctorId"));
        if (json.has("startDate")) settlement.setStartDate(json.getString("startDate"));
        if (json.has("endDate")) settlement.setEndDate(json.getString("endDate"));
        if (json.has("totalReportsCount")) settlement.setTotalReportsCount(json.getInt("totalReportsCount"));
        if (json.has("subtotal")) settlement.setSubtotal(json.getDouble("subtotal"));
        if (json.has("gstAmount")) settlement.setGstAmount(json.getDouble("gstAmount"));
        if (json.has("totalAmount")) settlement.setTotalAmount(json.getDouble("totalAmount"));
        if (json.has("paymentStatus")) settlement.setPaymentStatus(json.getString("paymentStatus"));
        if (json.has("paymentDate")) settlement.setPaymentDate(json.getString("paymentDate"));
        if (json.has("paymentMethod")) settlement.setPaymentMethod(json.getString("paymentMethod"));
        if (json.has("bankDetails")) settlement.setBankDetails(json.getString("bankDetails"));
        if (json.has("notes")) settlement.setNotes(json.getString("notes"));
        return settlement;
    }

    private JSONObject commissionToJson(DoctorCommission commission) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", commission.getId());
        json.put("doctorId", commission.getDoctorId());
        json.put("testId", commission.getTestId());
        json.put("subTestId", commission.getSubTestId());
        json.put("commissionRate", commission.getCommissionRate());
        json.put("commissionType", commission.getCommissionType().name());
        json.put("fixedAmount", commission.getFixedAmount());
        json.put("status", commission.getStatus());
        json.put("createdAt", commission.getCreatedAt());
        json.put("updatedAt", commission.getUpdatedAt());
        return json;
    }

    private DoctorCommission jsonToCommission(JSONObject json) throws JSONException {
        DoctorCommission commission = new DoctorCommission();
        if (json.has("doctorId")) commission.setDoctorId(json.getInt("doctorId"));
        if (json.has("testId")) commission.setTestId(json.getInt("testId"));
        if (json.has("subTestId")) commission.setSubTestId(json.getInt("subTestId"));
        if (json.has("commissionRate")) commission.setCommissionRate(json.getDouble("commissionRate"));
        if (json.has("commissionType")) {
            commission.setCommissionType(DoctorCommission.DoctorCommissionType.valueOf(json.getString("commissionType")));
        }
        if (json.has("fixedAmount")) commission.setFixedAmount(json.getDouble("fixedAmount"));
        if (json.has("status")) commission.setStatus(json.getString("status"));
        return commission;
    }

    private JSONArray settlementsToJsonArray(List<SettlementReport> settlements) throws JSONException {
        JSONArray array = new JSONArray();
        for (SettlementReport settlement : settlements) {
            array.put(settlementToJson(settlement));
        }
        return array;
    }

    private JSONArray commissionsToJsonArray(List<DoctorCommission> commissions) throws JSONException {
        JSONArray array = new JSONArray();
        for (DoctorCommission commission : commissions) {
            array.put(commissionToJson(commission));
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
