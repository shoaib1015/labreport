package labreport.server;

import labreport.model.Doctor;
import labreport.service.DoctorService;
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
 * REST API Controller for Doctor Management
 * Endpoints:
 * - GET /api/doctors - Get all doctors
 * - GET /api/doctors?search=name - Search doctors
 * - GET /api/doctors/{id} - Get doctor by ID
 * - POST /api/doctors - Create doctor
 * - PUT /api/doctors/{id} - Update doctor
 * - DELETE /api/doctors/{id} - Delete doctor
 */
public class DoctorController implements HttpHandler {
    private DoctorService doctorService;

    public DoctorController(DatabaseManager dbManager) {
        this.doctorService = new DoctorService(dbManager);
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
                handlePost(exchange);
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
        // Extract ID from path: /api/doctors/{id}
        String[] pathParts = path.split("/");
        
        if (pathParts.length > 3 && !pathParts[3].isEmpty()) {
            // GET /api/doctors/{id}
            try {
                int doctorId = Integer.parseInt(pathParts[3]);
                Doctor doctor = doctorService.getDoctorById(doctorId);
                
                if (doctor != null) {
                    sendJsonResponse(exchange, 200, doctorToJson(doctor).toString());
                } else {
                    sendErrorResponse(exchange, 404, "Doctor not found");
                }
            } catch (NumberFormatException e) {
                sendErrorResponse(exchange, 400, "Invalid doctor ID");
            }
        } else if (query != null && query.contains("search=")) {
            // GET /api/doctors?search=name
            String searchTerm = query.split("search=")[1];
            searchTerm = java.net.URLDecoder.decode(searchTerm, StandardCharsets.UTF_8.name());
            
            List<Doctor> doctors = doctorService.searchDoctorsByName(searchTerm);
            sendJsonResponse(exchange, 200, doctorsToJsonArray(doctors).toString());
        } else {
            // GET /api/doctors - Get all doctors
            List<Doctor> doctors = doctorService.getAllDoctors();
            sendJsonResponse(exchange, 200, doctorsToJsonArray(doctors).toString());
        }
    }

    /**
     * Handle POST requests - Create doctor
     */
    private void handlePost(HttpExchange exchange) throws IOException {
        try {
            String requestBody = readRequestBody(exchange);
            JSONObject json = new JSONObject(requestBody);
            
            Doctor doctor = jsonToDoctor(json);
            int doctorId = doctorService.createDoctor(doctor);
            
            if (doctorId > 0) {
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("message", "Doctor created successfully");
                response.put("id", doctorId);
                sendJsonResponse(exchange, 201, response.toString());
            } else {
                sendErrorResponse(exchange, 500, "Failed to create doctor");
            }
        } catch (JSONException e) {
            sendErrorResponse(exchange, 400, "Invalid JSON: " + e.getMessage());
        }
    }

    /**
     * Handle PUT requests - Update doctor
     */
    private void handlePut(HttpExchange exchange, String path) throws IOException {
        try {
            String[] pathParts = path.split("/");
            
            if (pathParts.length <= 3 || pathParts[3].isEmpty()) {
                sendErrorResponse(exchange, 400, "Missing doctor ID");
                return;
            }
            
            int doctorId = Integer.parseInt(pathParts[3]);
            String requestBody = readRequestBody(exchange);
            JSONObject json = new JSONObject(requestBody);
            
            Doctor doctor = jsonToDoctor(json);
            doctor.setId(doctorId);
            
            if (doctorService.updateDoctor(doctor)) {
                sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Doctor updated successfully\"}");
            } else {
                sendErrorResponse(exchange, 500, "Failed to update doctor");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid doctor ID");
        } catch (JSONException e) {
            sendErrorResponse(exchange, 400, "Invalid JSON: " + e.getMessage());
        }
    }

    /**
     * Handle DELETE requests - Delete doctor
     */
    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        try {
            String[] pathParts = path.split("/");
            
            if (pathParts.length <= 3 || pathParts[3].isEmpty()) {
                sendErrorResponse(exchange, 400, "Missing doctor ID");
                return;
            }
            
            int doctorId = Integer.parseInt(pathParts[3]);
            
            if (doctorService.deleteDoctor(doctorId)) {
                sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Doctor deleted successfully\"}");
            } else {
                sendErrorResponse(exchange, 500, "Failed to delete doctor");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid doctor ID");
        }
    }

    /**
     * Convert Doctor to JSON
     */
    private JSONObject doctorToJson(Doctor doctor) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", doctor.getId());
        json.put("doctorName", doctor.getDoctorName());
        json.put("specialization", doctor.getSpecialization());
        json.put("qualification", doctor.getQualification());
        json.put("experience", doctor.getExperience());
        json.put("registrationNumber", doctor.getRegistrationNumber());
        json.put("primaryPhone", doctor.getPrimaryPhone());
        json.put("secondaryPhone", doctor.getSecondaryPhone());
        json.put("email", doctor.getEmail());
        json.put("fax", doctor.getFax());
        json.put("clinicName", doctor.getClinicName());
        json.put("clinicAddress", doctor.getClinicAddress());
        json.put("clinicCity", doctor.getClinicCity());
        json.put("clinicState", doctor.getClinicState());
        json.put("clinicPhone", doctor.getClinicPhone());
        json.put("clinicEmail", doctor.getClinicEmail());
        json.put("defaultCommissionRate", doctor.getDefaultCommissionRate());
        json.put("settlementType", doctor.getSettlementType());
        json.put("notes", doctor.getNotes());
        json.put("status", doctor.getStatus());
        json.put("createdAt", doctor.getCreatedAt());
        json.put("updatedAt", doctor.getUpdatedAt());
        return json;
    }

    /**
     * Convert JSON to Doctor
     */
    private Doctor jsonToDoctor(JSONObject json) throws JSONException {
        Doctor doctor = new Doctor();
        
        if (json.has("doctorName")) doctor.setDoctorName(json.getString("doctorName"));
        if (json.has("specialization")) doctor.setSpecialization(json.getString("specialization"));
        if (json.has("qualification")) doctor.setQualification(json.getString("qualification"));
        if (json.has("experience")) doctor.setExperience(json.getInt("experience"));
        if (json.has("registrationNumber")) doctor.setRegistrationNumber(json.getString("registrationNumber"));
        if (json.has("primaryPhone")) doctor.setPrimaryPhone(json.getString("primaryPhone"));
        if (json.has("secondaryPhone")) doctor.setSecondaryPhone(json.getString("secondaryPhone"));
        if (json.has("email")) doctor.setEmail(json.getString("email"));
        if (json.has("fax")) doctor.setFax(json.getString("fax"));
        if (json.has("clinicName")) doctor.setClinicName(json.getString("clinicName"));
        if (json.has("clinicAddress")) doctor.setClinicAddress(json.getString("clinicAddress"));
        if (json.has("clinicCity")) doctor.setClinicCity(json.getString("clinicCity"));
        if (json.has("clinicState")) doctor.setClinicState(json.getString("clinicState"));
        if (json.has("clinicPhone")) doctor.setClinicPhone(json.getString("clinicPhone"));
        if (json.has("clinicEmail")) doctor.setClinicEmail(json.getString("clinicEmail"));
        if (json.has("defaultCommissionRate")) doctor.setDefaultCommissionRate(json.getDouble("defaultCommissionRate"));
        if (json.has("settlementType")) doctor.setSettlementType(json.getString("settlementType"));
        if (json.has("notes")) doctor.setNotes(json.getString("notes"));
        if (json.has("status")) doctor.setStatus(json.getString("status"));
        
        return doctor;
    }

    /**
     * Convert list of doctors to JSON array
     */
    private JSONArray doctorsToJsonArray(List<Doctor> doctors) throws JSONException {
        JSONArray array = new JSONArray();
        for (Doctor doctor : doctors) {
            array.put(doctorToJson(doctor));
        }
        return array;
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
