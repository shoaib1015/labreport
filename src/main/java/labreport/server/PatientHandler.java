package labreport.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import labreport.auth.PatientService;
import labreport.auth.TestOrderComponentService;
import labreport.auth.PatientService.CreatePatientRequest;
import labreport.auth.PatientService.CreatePatientResponse;
import labreport.logging.AppLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.Map;
import java.util.logging.Logger;

public class PatientHandler implements HttpHandler {

    private static final Logger log = AppLogger.getLogger();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase();
            String path = exchange.getRequestURI().getPath();

            if ("POST".equals(method) && path.endsWith("/api/patients")) {
                handleCreatePatient(exchange);
            } else if("GET".equals(method) && path.matches("/api/patients/dashboard/stats")) {
                // Handle dashboard stats request
                // This is a placeholder. You would implement the logic to fetch and return the dashboard stats here.
                log.info("Received request for patient dashboard stats");
                handleGetPatientDashboardStats(exchange);
            } else if("GET".equals(method) && path.equals("/api/patients/all")) {
                // Handle all patients request
                log.info("Received request to fetch all patients");
                handleGetAllPatients(exchange);
            } else if ("GET".equals(method) && path.equals("/api/patients")) {
                handleSearchPatients(exchange);
            } else if("GET".equals(method) && path.matches("/api/patients/recent")) {
                // Handle list patients request (not implemented in this snippet)
                log.info("Received request to list patients");
                // You would implement the logic to fetch and return the list of patients here.
                handleListRecentPatients(exchange,path);

            } else if ("GET".equals(method) && path.matches(".*/api/patients/\\d+/test-orders")) {
                int patientId = extractPatientIdFromPath(path);
                handleGetPatientTestOrders(exchange, patientId);
            } else if ("PUT".equals(method) && path.matches(".*/api/patients/\\d+/test-entry")) {
                int patientId = extractPatientIdFromPath(path);
                handleSaveTestEntry(exchange, patientId);
            } else if ("PUT".equals(method) && path.matches(".*/api/patients/\\d+")) {
                int patientId = extractId(path);
                handleUpdatePatient(exchange, patientId);
            } else if ("GET".equals(method) && path.matches(".*/api/patients/\\d+")) {
                int patientId = extractId(path);
                handleGetPatient(exchange, patientId);
            }else if ("DELETE".equals(method) && path.matches(".*/api/patients/\\d+")) {
                int patientId = extractId(path);
                handleDeletePatient(exchange, patientId);
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
            }

        } catch (Exception e) {
            log.severe("Patient handler error: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Internal server error");
        }
    }

    private void handleCreatePatient(HttpExchange exchange) throws IOException {
        try {
            String body = readBody(exchange.getRequestBody());
            log.info("Create patient request body: " + body);

            CreatePatientRequest request = parseRequest(body);
            CreatePatientResponse response = PatientService.createPatient(request);

            String jsonResponse = objectToJson(response);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jsonResponse.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
            }

            log.info("Patient created successfully");

        } catch (Exception e) {
            log.severe("Failed to create patient: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to create patient: " + e.getMessage());
        }
    }

    private void handleUpdatePatient(HttpExchange exchange, int patientId) throws IOException {
        try {
            String body = readBody(exchange.getRequestBody());
            log.info("Update patient request body: " + body);

            CreatePatientRequest request = parseRequest(body);
            CreatePatientResponse response = PatientService.updatePatient(patientId, request);

            String jsonResponse = objectToJson(response);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jsonResponse.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
            }

            log.info("Patient updated successfully: id=" + patientId);

        } catch (Exception e) {
            log.severe("Failed to update patient: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to update patient: " + e.getMessage());
        }
    }

    private void handleGetPatient(HttpExchange exchange, int patientId) throws IOException {
        try {
            Map<String, String> patient = PatientService.getPatientById(patientId);

            if (patient.isEmpty()) {
                sendErrorResponse(exchange, 404, "Patient not found");
                return;
            }

            String jsonResponse = mapToJson(patient);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jsonResponse.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
            }

            log.info("Patient fetched successfully: id=" + patientId);

        } catch (Exception e) {
            log.severe("Failed to get patient: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to fetch patient: " + e.getMessage());
        }
    }

    private void handleSearchPatients(HttpExchange exchange) throws IOException {
        try {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = query != null ? parseQueryParams(query) : java.util.Collections.emptyMap();

            String search = params.get("search");
            String gender = params.get("gender");
            String createdAt = params.get("created_at");

            if ((search == null || search.isEmpty()) && (gender == null || gender.isEmpty()) && (createdAt == null || createdAt.isEmpty())) {
                String jsonResponse = "{\"patients\":[]}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
                }
                return;
            }

            String jsonResponse = PatientService.searchPatientsJson(search, gender, createdAt);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
            }
        } catch (SQLException e) {
            log.severe("Failed to search patients: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to search patients: " + e.getMessage());
        }
    }

    private CreatePatientRequest parseRequest(String json) {
        CreatePatientRequest request = new CreatePatientRequest();
        
        // Manual JSON parsing
        request.name = extractJsonString(json, "name");
        request.dob = extractJsonString(json, "dob");
        request.gender = extractJsonString(json, "gender");
        request.contact_phone = extractJsonString(json, "contact_phone");
        request.contact_email = extractJsonString(json, "contact_email");
        request.address = extractJsonString(json, "address");
        request.priority = extractJsonString(json, "priority");
        request.notes = extractJsonString(json, "notes");
        
        String refDocIdStr = extractJsonString(json, "referring_doctor_id");
        if (refDocIdStr != null && !refDocIdStr.isEmpty() && !refDocIdStr.equals("null")) {
            try {
                request.referring_doctor_id = Integer.parseInt(refDocIdStr);
            } catch (NumberFormatException e) {
                // Leave as null
            }
        }
        
        String createdByStr = extractJsonString(json, "created_by");
        if (createdByStr != null && !createdByStr.isEmpty()) {
            try {
                request.created_by = Integer.parseInt(createdByStr);
            } catch (NumberFormatException e) {
                request.created_by = 1; // Default
            }
        }
        
        // Parse order_panels array
        request.order_panels = extractJsonArray(json, "order_panels");
        
        return request;
    }

    private String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int index = json.indexOf(searchKey);
        if (index == -1) {
            return null;
        }
        
        int startIndex = index + searchKey.length();
        while (startIndex < json.length() && (json.charAt(startIndex) == ' ' || json.charAt(startIndex) == '\n')) {
            startIndex++;
        }
        
        if (startIndex >= json.length()) {
            return null;
        }
        
        char firstChar = json.charAt(startIndex);
        if (firstChar == '"') {
            // String value
            int endIndex = json.indexOf('"', startIndex + 1);
            if (endIndex == -1) {
                return null;
            }
            return json.substring(startIndex + 1, endIndex);
        } else if (firstChar == 'n') {
            // null value
            return null;
        } else {
            // Number or boolean
            int endIndex = startIndex;
            while (endIndex < json.length() && json.charAt(endIndex) != ',' && json.charAt(endIndex) != '}') {
                endIndex++;
            }
            return json.substring(startIndex, endIndex).trim();
        }
    }

    private java.util.List<Integer> extractJsonArray(String json, String key) {
        java.util.List<Integer> list = new java.util.ArrayList<>();
        String searchKey = "\"" + key + "\":";
        int index = json.indexOf(searchKey);
        if (index == -1) {
            return list;
        }
        
        int arrayStart = json.indexOf('[', index);
        int arrayEnd = json.indexOf(']', arrayStart);
        if (arrayStart == -1 || arrayEnd == -1) {
            return list;
        }
        
        String arrayStr = json.substring(arrayStart + 1, arrayEnd);
        if (arrayStr.trim().isEmpty()) {
            return list;
        }
        
        String[] items = arrayStr.split(",");
        for (String item : items) {
            try {
                list.add(Integer.parseInt(item.trim()));
            } catch (NumberFormatException e) {
                // Skip invalid items
            }
        }
        
        return list;
    }

    private String objectToJson(CreatePatientResponse response) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        // validationResult
        json.append("\"validationResult\":{");
        json.append("\"valid\":").append(response.validationResult.valid).append(",");
        json.append("\"errors\":[");
        for (int i = 0; i < response.validationResult.errors.size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(escapeJson(response.validationResult.errors.get(i))).append("\"");
        }
        json.append("]");
        json.append("},");
        
        // sqlTransaction
        json.append("\"sqlTransaction\":\"").append(escapeJson(response.sqlTransaction)).append("\",");
        
        // createdObjects
        json.append("\"createdObjects\":").append(mapToJson(response.createdObjects));
        
        json.append("}");
        return json.toString();
    }

    private String mapToJson(Map<?, ?> map) {
        StringBuilder json = new StringBuilder("{");
        Object[] keys = map.keySet().toArray();
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) json.append(",");
            Object key = keys[i];
            Object value = map.get(key);
            json.append("\"").append(key).append("\":");
            json.append(valueToJson(value));
        }
        json.append("}");
        return json.toString();
    }

    private String valueToJson(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        } else if (value instanceof Number) {
            return value.toString();
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof java.util.List) {
            StringBuilder json = new StringBuilder("[");
            java.util.List<?> list = (java.util.List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) json.append(",");
                json.append(valueToJson(list.get(i)));
            }
            json.append("]");
            return json.toString();
        } else if (value instanceof Map) {
            return mapToJson((Map<?, ?>) value);
        } else {
            return "\"" + escapeJson(value.toString()) + "\"";
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        String errorResponse = "{\"error\":\"" + escapeJson(message) + "\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, errorResponse.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(errorResponse.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private int extractId(String path) {
        String[] parts = path.split("/");
        return Integer.parseInt(parts[parts.length - 1]);
    }

    private String readBody(InputStream is) throws IOException {
        try (Scanner scanner = new Scanner(is).useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private void handleGetPatientDashboardStats(HttpExchange exchange) throws IOException {
        try {
            String json = PatientService.getDashboardStatsJson();
            log.info("Dashboard stats fetched successfully" + json);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
        } catch (SQLException e) {
            log.severe("Failed to get patient: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to fetch patient: " + e.getMessage());
        }    
    }

    private void handleGetAllPatients(HttpExchange exchange) throws IOException {
        try {
            String json = PatientService.getAllPatientsJson();
            log.info("All patients fetched successfully");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
        } catch (SQLException e) {
            log.severe("Failed to get all patients: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to fetch patients: " + e.getMessage());
        }
    }

    private void handleListRecentPatients(HttpExchange exchange, String path) throws IOException {
        try {
            int limit = 5;
            String sort = "created_at";
            String order = "DESC";

            String query = exchange.getRequestURI().getQuery();
            if (query != null) {
                Map<String, String> params = parseQueryParams(query);

                String limitParam = params.get("limit");
                if (limitParam != null) {
                    try {
                        limit = Integer.parseInt(limitParam);
                    } catch (NumberFormatException ignored) {
                        // Keep default limit
                    }
                }

                String sortParam = params.get("sort");
                if (sortParam != null && !sortParam.isEmpty()) {
                    sort = sortParam;
                }

                String orderParam = params.get("order");
                if (orderParam != null && !orderParam.isEmpty()) {
                    order = orderParam;
                }
            }

            String json = PatientService.getRecentPatientsJson(limit, sort, order);
            log.info("Recent patients fetched successfully" + json);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
        } catch (SQLException e) {
            log.severe("Failed to get patient: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to fetch patient: " + e.getMessage());
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new java.util.HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx > 0 && idx < pair.length() - 1) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                params.put(key, value);
            }
        }
        return params;
    }

    private void handleGetPatientTestOrders(HttpExchange exchange, int patientId) throws IOException {
        try {
            String json = PatientService.getPatientTestOrdersJson(patientId);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
        } catch (SQLException e) {
            log.severe("Failed to fetch test orders: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to fetch test orders: " + e.getMessage());
        }
    }

    private int extractPatientIdFromPath(String path) {
        // Extract patient ID from path like /api/patients/123/test-orders
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("patients".equals(parts[i]) && i + 1 < parts.length) {
                try {
                    return Integer.parseInt(parts[i + 1]);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private void handleSaveTestEntry(HttpExchange exchange, int patientId) throws IOException {
        try {
            String body = readBody(exchange.getRequestBody());
            log.info("Save test entry request for patient_id=" + patientId + ": " + body);

            // Parse top-level fields
            String sampleCollectedAt = extractJsonString(body, "sampleCollectedAt");
            String status = extractJsonString(body, "status");
            String notes = extractJsonString(body, "notes");
            String testOrderIdStr = extractJsonString(body, "testOrderId");

            if (testOrderIdStr == null || testOrderIdStr.isEmpty()) {
                sendErrorResponse(exchange, 400, "testOrderId is required");
                return;
            }

            int testOrderId = Integer.parseInt(testOrderIdStr);

            // Update test_order
            PatientService.updateTestOrder(testOrderId, sampleCollectedAt, status, notes);

            // Parse and update components array
            int compStart = body.indexOf("\"components\"");
            if (compStart != -1) {
                int arrayStart = body.indexOf('[', compStart);
                int arrayEnd = body.lastIndexOf(']');
                if (arrayStart != -1 && arrayEnd > arrayStart) {
                    String arrayContent = body.substring(arrayStart + 1, arrayEnd);
                    // Split into individual objects by finding each {...}
                    int depth = 0;
                    int objStart = -1;
                    for (int i = 0; i < arrayContent.length(); i++) {
                        char c = arrayContent.charAt(i);
                        if (c == '{') {
                            if (depth == 0) objStart = i;
                            depth++;
                        } else if (c == '}') {
                            depth--;
                            if (depth == 0 && objStart != -1) {
                                String obj = arrayContent.substring(objStart, i + 1);
                                String idStr = extractJsonString(obj, "id");
                                String resultValue = extractJsonString(obj, "resultValue");
                                String flag = extractJsonString(obj, "flag");
                                if (idStr != null && !idStr.isEmpty()) {
                                    int componentId = Integer.parseInt(idStr);
                                    TestOrderComponentService.updateComponentResult(
                                        componentId,
                                        resultValue != null ? resultValue : "",
                                        flag != null ? flag : "Normal"
                                    );
                                }
                                objStart = -1;
                            }
                        }
                    }
                }
            }

            String response = "{\"success\":true,\"message\":\"Test entry saved successfully\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }

            log.info("Test entry saved successfully for patient_id=" + patientId + ", testOrderId=" + testOrderId);

        } catch (Exception e) {
            log.severe("Failed to save test entry: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(exchange, 500, "Failed to save test entry: " + e.getMessage());
        }
    }

    private void handleDeletePatient(HttpExchange exchange, int patientId) throws IOException {
        try {
            PatientService.deletePatient(patientId);
            String response = "{\"success\":true,\"message\":\"Patient deleted successfully\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
            log.info("Patient deleted successfully: id=" + patientId);
        } catch (Exception e) {
            log.severe("Failed to delete patient: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to delete patient: " + e.getMessage());
        }
    }
}


