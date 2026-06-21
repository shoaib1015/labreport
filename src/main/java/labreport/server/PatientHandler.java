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
            } else if ("GET".equals(method) && path.matches("/api/patients/dashboard/stats")) {
                // Handle dashboard stats request
                // This is a placeholder. You would implement the logic to fetch and return the
                // dashboard stats here.
                log.info("Received request for patient dashboard stats");
                handleGetPatientDashboardStats(exchange);
            } else if ("GET".equals(method) && path.equals("/api/patients/all")) {
                // Handle all patients request
                handleGetAllPatients(exchange);
            } else if ("GET".equals(method) && path.equals("/api/patients")) {
                handleSearchPatients(exchange);
            } else if ("GET".equals(method) && path.matches("/api/patients/recent")) {
                // Handle list patients request (not implemented in this snippet)
                log.info("Received request to list patients");
                // You would implement the logic to fetch and return the list of patients here.
                handleListRecentPatients(exchange, path);

            } else if ("GET".equals(method) && path.matches(".*/api/patients/[A-Za-z0-9-]+/test-orders")) {
                String patientId = extractPatientIdFromPath(path);
                handleGetPatientTestOrders(exchange, patientId);
            } else if ("PUT".equals(method) && path.matches(".*/api/patients/[A-Za-z0-9-]+/test-entry")) {
                String patientId = extractPatientIdFromPath(path);
                handleSaveTestEntry(exchange, patientId);
            } else if ("PUT".equals(method) && path.matches(".*/api/patients/[A-Za-z0-9-]+")) {
                String patientId = extractId(path);
                handleUpdatePatient(exchange, patientId);
            } else if ("GET".equals(method) && path.matches(".*/api/patients/[A-Za-z0-9-]+")) {
                String patientId = extractId(path);
                log.info("patient_id:" + patientId);
                handleGetPatient(exchange, patientId);
            } else if ("DELETE".equals(method) && path.matches(".*/api/patients/[A-Za-z0-9-]+")) {
                String patientId = extractId(path);
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

            if (!response.validationResult.valid) {
                int status = response.validationResult.errors.stream()
                        .anyMatch(err -> err.startsWith("Database error:"))
                                ? HttpURLConnection.HTTP_INTERNAL_ERROR
                                : HttpURLConnection.HTTP_BAD_REQUEST;
                exchange.sendResponseHeaders(status, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
                log.warning("Patient creation failed: " + response.validationResult.errors);
            } else {
                exchange.sendResponseHeaders(200, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
                log.info("Patient created successfully");
            }

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
            }

        } catch (Exception e) {
            log.severe("Failed to create patient: " + e.getMessage());
            sendErrorResponse(exchange, 500, "Failed to create patient: " + e.getMessage());
        }
    }

    private void handleUpdatePatient(HttpExchange exchange, String patientId) throws IOException {
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

    private void handleGetPatient(HttpExchange exchange, String patientId) throws IOException {
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

            log.info("Search patients with parameters: search=" + search + ", gender=" + gender + ", created_at="
                    + createdAt);

            if ((search == null || search.isEmpty()) && (gender == null || gender.isEmpty())
                    && (createdAt == null || createdAt.isEmpty())) {
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

        Double commissionPercent = null;
        String commissionPercentStr = extractJsonString(json, "commission_percent");
        if (commissionPercentStr != null && !commissionPercentStr.isEmpty()) {
            try {
                commissionPercent = Double.parseDouble(commissionPercentStr);
            } catch (NumberFormatException e) {
                // Leave as null
            }
        }
        request.commission_percent = commissionPercent;
        log.info("Parsed commission_percent: " + commissionPercent);

        Double amountPaid = 0.0;
        String amountPaidStr = extractJsonString(json, "amount_paid");
        if (amountPaidStr != null && !amountPaidStr.isEmpty() && !amountPaidStr.equals("null")) {
            try {
                amountPaid = Double.parseDouble(amountPaidStr);
            } catch (NumberFormatException e) {
                amountPaid = 0.0;
            }
        }
        request.amount_paid = amountPaid;
        log.info("Parsed amount_paid: " + amountPaid);

        // Parse order_panels array (supports [1,2,3] or
        // [{"panelId":1,"commission_percent":10}, ...])
        request.order_panels = extractOrderPanels(json, "order_panels");

        return request;
    }

    private java.util.List<PatientService.PanelOrder> extractOrderPanels(String json, String key) {
        java.util.List<PatientService.PanelOrder> panelOrders = new java.util.ArrayList<>();
        String searchKey = "\"" + key + "\":";
        int index = json.indexOf(searchKey);
        if (index == -1) {
            return panelOrders;
        }

        int arrayStart = json.indexOf('[', index);
        int arrayEnd = json.indexOf(']', arrayStart);
        if (arrayStart == -1 || arrayEnd == -1) {
            return panelOrders;
        }

        String arrayStr = json.substring(arrayStart + 1, arrayEnd).trim();
        if (arrayStr.isEmpty()) {
            return panelOrders;
        }

        if (arrayStr.matches("^[0-9,\\s]*$")) {
            for (String item : arrayStr.split(",")) {
                try {
                    if (!item.trim().isEmpty()) {
                        PatientService.PanelOrder panelOrder = new PatientService.PanelOrder();
                        panelOrder.panelId = Integer.parseInt(item.trim());
                        panelOrders.add(panelOrder);
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid items
                }
            }
            return panelOrders;
        }

        int cursor = arrayStart + 1;
        while (cursor < arrayEnd) {
            int objectStart = json.indexOf('{', cursor);
            if (objectStart == -1 || objectStart >= arrayEnd) {
                break;
            }
            int objectEnd = json.indexOf('}', objectStart);
            if (objectEnd == -1 || objectEnd > arrayEnd) {
                break;
            }

            String objectJson = json.substring(objectStart, objectEnd + 1);
            String panelIdStr = extractJsonString(objectJson, "panelId");
            if (panelIdStr == null) {
                panelIdStr = extractJsonString(objectJson, "panel_id");
            }

            if (panelIdStr != null) {
                try {
                    PatientService.PanelOrder panelOrder = new PatientService.PanelOrder();
                    panelOrder.panelId = Integer.parseInt(panelIdStr);

                    String commissionStr = extractJsonString(objectJson, "commissionPercent");
                    if (commissionStr == null) {
                        commissionStr = extractJsonString(objectJson, "commission_percent");
                    }
                    if (commissionStr != null && !commissionStr.isEmpty()) {
                        try {
                            panelOrder.commissionPercent = Double.parseDouble(commissionStr);
                        } catch (NumberFormatException ignored) {
                            panelOrder.commissionPercent = null;
                        }
                    }

                    String discountStr = extractJsonString(objectJson, "discountApplied");
                    if (discountStr == null) {
                        discountStr = extractJsonString(objectJson, "discount_applied");
                    }
                    if (discountStr != null && !discountStr.isEmpty()) {
                        try {
                            panelOrder.discount_applied = Double.parseDouble(discountStr);
                        } catch (NumberFormatException ignored) {
                            panelOrder.discount_applied = 0.0;
                        }
                    } else {
                        panelOrder.discount_applied = 0.0;
                    }

                    panelOrders.add(panelOrder);
                } catch (NumberFormatException e) {
                    // Skip invalid panel IDs
                }
            }

            cursor = objectEnd + 1;
        }

        return panelOrders;
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

    private java.util.List<Integer> extractOrderPanelIds(String json, String key) {
        java.util.List<Integer> panelIds = new java.util.ArrayList<>();
        String searchKey = "\"" + key + "\":";
        int index = json.indexOf(searchKey);
        if (index == -1) {
            return panelIds;
        }

        int arrayStart = json.indexOf('[', index);
        int arrayEnd = json.indexOf(']', arrayStart);
        if (arrayStart == -1 || arrayEnd == -1) {
            return panelIds;
        }

        String arrayStr = json.substring(arrayStart + 1, arrayEnd).trim();
        if (arrayStr.isEmpty()) {
            return panelIds;
        }

        // If the array contains numeric values only, parse them directly.
        if (arrayStr.matches("^[0-9,\\s]*$")) {
            for (String item : arrayStr.split(",")) {
                try {
                    if (!item.trim().isEmpty()) {
                        panelIds.add(Integer.parseInt(item.trim()));
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid items
                }
            }
            return panelIds;
        }

        // Otherwise attempt to parse objects in the array and extract panelId /
        // panel_id.
        int cursor = arrayStart + 1;
        while (cursor < arrayEnd) {
            int objectStart = json.indexOf('{', cursor);
            if (objectStart == -1 || objectStart >= arrayEnd) {
                break;
            }
            int objectEnd = json.indexOf('}', objectStart);
            if (objectEnd == -1 || objectEnd > arrayEnd) {
                break;
            }

            String objectStr = json.substring(objectStart + 1, objectEnd);
            Integer id = null;
            String panelIdStr = extractJsonString("{" + objectStr + "}", "panelId");
            if (panelIdStr == null) {
                panelIdStr = extractJsonString("{" + objectStr + "}", "panel_id");
            }
            if (panelIdStr != null) {
                try {
                    id = Integer.parseInt(panelIdStr);
                } catch (NumberFormatException e) {
                    id = null;
                }
            }
            if (id != null) {
                panelIds.add(id);
            }
            cursor = objectEnd + 1;
        }

        return panelIds;
    }

    private String objectToJson(CreatePatientResponse response) {
        StringBuilder json = new StringBuilder();
        json.append("{");

        // validationResult
        json.append("\"validationResult\":{");
        json.append("\"valid\":").append(response.validationResult.valid).append(",");
        json.append("\"errors\":[");
        for (int i = 0; i < response.validationResult.errors.size(); i++) {
            if (i > 0)
                json.append(",");
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
            if (i > 0)
                json.append(",");
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
                if (i > 0)
                    json.append(",");
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
        if (value == null)
            return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractId(String path) {
        String[] parts = path.split("/");
        return (parts[parts.length - 1]);
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
            String dateFrom = null;
            String dateTo = null;

            // Parse query parameters for date range
            String query = exchange.getRequestURI().getQuery();
            if (query != null) {
                Map<String, String> params = parseQueryParams(query);
                dateFrom = params.get("dateFrom");
                dateTo = params.get("dateTo");
            }

            String json = PatientService.getAllPatientsJson(dateFrom, dateTo);
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

    private void handleGetPatientTestOrders(HttpExchange exchange, String patientId) throws IOException {
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

    private String extractPatientIdFromPath(String path) {
        // Extract patient ID from path like /api/patients/123/test-orders
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("patients".equals(parts[i]) && i + 1 < parts.length) {
                try {
                    log.info("Extracted patient ID from path: " + parts[i + 1]);
                    return parts[i + 1];
                } catch (NumberFormatException e) {
                    log.info("Invalid patient ID in path: " + path);
                    return " "; // Invalid ID
                }
            }
        }
        return " ";
    }

    private void handleSaveTestEntry(HttpExchange exchange, String patientId) throws IOException {
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
                            if (depth == 0)
                                objStart = i;
                            depth++;
                        } else if (c == '}') {
                            depth--;
                            if (depth == 0 && objStart != -1) {
                                String obj = arrayContent.substring(objStart, i + 1);
                                String idStr = extractJsonString(obj, "id");
                                String componentIdStr = extractJsonString(obj, "componentId");
                                String componentName = extractJsonString(obj, "componentName");
                                if (componentName == null) {
                                    componentName = extractJsonString(obj, "component_name");
                                }
                                String unit = extractJsonString(obj, "unit");
                                String referenceRange = extractJsonString(obj, "referenceRange");
                                if (referenceRange == null) {
                                    referenceRange = extractJsonString(obj, "reference_range");
                                }
                                String resultValue = extractJsonString(obj, "resultValue");
                                String flag = extractJsonString(obj, "flag");

                                int componentId = 0;
                                if (componentIdStr != null && !componentIdStr.isEmpty()) {
                                    try {
                                        componentId = Integer.parseInt(componentIdStr);
                                    } catch (NumberFormatException ignored) {
                                        componentId = 0;
                                    }
                                }

                                if (idStr != null && !idStr.isEmpty()) {
                                    int testOrderComponentId = Integer.parseInt(idStr);
                                    TestOrderComponentService.updateComponentResult(
                                            testOrderComponentId,
                                            resultValue != null ? resultValue : "",
                                            flag != null ? flag : "Normal");
                                } else if (componentName != null && !componentName.isEmpty()) {
                                    // Insert the parent component if it has its own value or no nested
                                    // subcomponents
                                    boolean insertedParent = false;
                                    int parentInsertedId = 0;

                                    // Detect nested subcomponents (support keys: subcomponents, children,
                                    // sub_components)
                                    int subStartIdx = obj.indexOf("\"subcomponents\"");
                                    if (subStartIdx == -1)
                                        subStartIdx = obj.indexOf("\"children\"");
                                    if (subStartIdx == -1)
                                        subStartIdx = obj.indexOf("\"sub_components\"");

                                    if ((resultValue != null && !resultValue.isEmpty()) || subStartIdx == -1) {
                                        // Insert parent component row (will also be shown if it has a result)
                                        TestOrderComponentService.insertComponentResult(
                                                testOrderId,
                                                componentId,
                                                componentName,
                                                unit != null ? unit : "",
                                                referenceRange != null ? referenceRange : "",
                                                resultValue != null ? resultValue : "",
                                                flag != null ? flag : "Normal");
                                        insertedParent = true;
                                    }

                                    // If nested subcomponents exist, parse and insert them as separate rows
                                    if (subStartIdx != -1) {
                                        int arrStart = obj.indexOf('[', subStartIdx);
                                        int arrEnd = obj.indexOf(']', arrStart);
                                        if (arrStart != -1 && arrEnd > arrStart) {
                                            String subArray = obj.substring(arrStart + 1, arrEnd);
                                            int depth2 = 0;
                                            int subObjStart = -1;
                                            for (int j = 0; j < subArray.length(); j++) {
                                                char ch = subArray.charAt(j);
                                                if (ch == '{') {
                                                    if (depth2 == 0)
                                                        subObjStart = j;
                                                    depth2++;
                                                } else if (ch == '}') {
                                                    depth2--;
                                                    if (depth2 == 0 && subObjStart != -1) {
                                                        String subObj = subArray.substring(subObjStart, j + 1);
                                                        // Extract subcomponent fields
                                                        String subIdStr = extractJsonString(subObj, "id");
                                                        String subComponentIdStr = extractJsonString(subObj,
                                                                "componentId");
                                                        String subName = extractJsonString(subObj, "componentName");
                                                        if (subName == null)
                                                            subName = extractJsonString(subObj, "component_name");
                                                        if (subName == null)
                                                            subName = extractJsonString(subObj, "name");
                                                        String subUnit = extractJsonString(subObj, "unit");
                                                        String subRef = extractJsonString(subObj, "referenceRange");
                                                        if (subRef == null)
                                                            subRef = extractJsonString(subObj, "reference_range");
                                                        String subResult = extractJsonString(subObj, "resultValue");
                                                        String subFlag = extractJsonString(subObj, "flag");

                                                        int subComponentId = 0;
                                                        if (subComponentIdStr != null && !subComponentIdStr.isEmpty()) {
                                                            try {
                                                                subComponentId = Integer.parseInt(subComponentIdStr);
                                                            } catch (NumberFormatException ignored) {
                                                                subComponentId = 0;
                                                            }
                                                        }

                                                        // Build display name combining parent and sub (keeps hierarchy
                                                        // visible)
                                                        String displayName = componentName + " — "
                                                                + (subName != null ? subName : "Subcomponent");

                                                        if (subIdStr != null && !subIdStr.isEmpty()) {
                                                            int subRowId = Integer.parseInt(subIdStr);
                                                            TestOrderComponentService.updateComponentResult(
                                                                    subRowId,
                                                                    subResult != null ? subResult : "",
                                                                    subFlag != null ? subFlag : "Normal");
                                                        } else {
                                                            TestOrderComponentService.insertComponentResult(
                                                                    testOrderId,
                                                                    subComponentId,
                                                                    displayName,
                                                                    subUnit != null ? subUnit : "",
                                                                    subRef != null ? subRef : "",
                                                                    subResult != null ? subResult : "",
                                                                    subFlag != null ? subFlag : "Normal");
                                                        }

                                                        subObjStart = -1;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                objStart = -1;
                            }
                        }
                    }
                }
            }

            // Parse removedComponentIds array if provided and delete them
            int remStart = body.indexOf("\"removedComponentIds\"");
            if (remStart != -1) {
                int arrayStart = body.indexOf('[', remStart);
                int arrayEnd = body.indexOf(']', arrayStart);
                if (arrayStart != -1 && arrayEnd > arrayStart) {
                    String arrayContent = body.substring(arrayStart + 1, arrayEnd);
                    String[] items = arrayContent.split(",");
                    for (String item : items) {
                        try {
                            String trimmed = item.trim();
                            if (trimmed.length() == 0)
                                continue;
                            int compId = Integer.parseInt(trimmed);
                            TestOrderComponentService.deleteComponentById(compId);
                        } catch (NumberFormatException nfe) {
                            // ignore invalid ids
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

    private void handleDeletePatient(HttpExchange exchange, String patientId) throws IOException {
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
