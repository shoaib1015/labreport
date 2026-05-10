package labreport.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import labreport.auth.PatientService;
import labreport.auth.PatientService.CreatePatientRequest;
import labreport.auth.PatientService.CreatePatientResponse;
import labreport.logging.AppLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
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
            } else if("GET".equals(method) && path.matches(".*/api/patients/dashboard/stats")) {
                // Handle dashboard stats request
                // This is a placeholder. You would implement the logic to fetch and return the dashboard stats here.
                String jsonResponse = "{\"total_patients\": 100, \"new_patients_today\": 5}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
                }
            } else if ("GET".equals(method) && path.matches(".*/api/patients/\\d+")) {
                
                int patientId = extractId(path);
                handleGetPatient(exchange, patientId);
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
}

