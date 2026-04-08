package labreport.server;

import labreport.model.Patient;
import labreport.service.PatientService;
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
 * REST API Controller for Patient Management
 * Endpoints:
 * - GET /api/patients - Get all patients
 * - GET /api/patients?search=name - Search patients by name
 * - GET /api/patients?searchPhone=phone - Search patients by phone
 * - GET /api/patients?searchDoctor=id - Search patients by referred doctor
 * - GET /api/patients/{id} - Get patient by ID
 * - POST /api/patients - Create patient
 * - PUT /api/patients/{id} - Update patient
 * - DELETE /api/patients/{id} - Delete patient
 */
public class PatientController implements HttpHandler {
    private PatientService patientService;

    public PatientController(DatabaseManager dbManager) {
        this.patientService = new PatientService(dbManager);
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
        String[] pathParts = path.split("/");
        
        if (pathParts.length > 3 && !pathParts[3].isEmpty()) {
            // GET /api/patients/{id}
            try {
                int patientId = Integer.parseInt(pathParts[3]);
                Patient patient = patientService.getPatientById(patientId);
                
                if (patient != null) {
                    sendJsonResponse(exchange, 200, patientToJson(patient).toString());
                } else {
                    sendErrorResponse(exchange, 404, "Patient not found");
                }
            } catch (NumberFormatException e) {
                sendErrorResponse(exchange, 400, "Invalid patient ID");
            }
        } else if (query != null && query.contains("search=")) {
            // GET /api/patients?search=name
            String searchTerm = query.split("search=")[1];
            searchTerm = java.net.URLDecoder.decode(searchTerm, StandardCharsets.UTF_8.name());
            
            List<Patient> patients = patientService.searchByName(searchTerm);
            sendJsonResponse(exchange, 200, patientsToJsonArray(patients).toString());
        } else if (query != null && query.contains("searchPhone=")) {
            // GET /api/patients?searchPhone=phone
            String phone = query.split("searchPhone=")[1];
            phone = java.net.URLDecoder.decode(phone, StandardCharsets.UTF_8.name());
            
            List<Patient> patients = patientService.searchByPhone(phone);
            sendJsonResponse(exchange, 200, patientsToJsonArray(patients).toString());
        } else if (query != null && query.contains("searchDoctor=")) {
            // GET /api/patients?searchDoctor=id
            try {
                String doctorIdStr = query.split("searchDoctor=")[1];
                int doctorId = Integer.parseInt(doctorIdStr);
                
                List<Patient> patients = patientService.searchByReferredDoctor(doctorId);
                sendJsonResponse(exchange, 200, patientsToJsonArray(patients).toString());
            } catch (NumberFormatException e) {
                sendErrorResponse(exchange, 400, "Invalid doctor ID");
            }
        } else {
            // GET /api/patients - Get all patients
            List<Patient> patients = patientService.getAllPatients();
            sendJsonResponse(exchange, 200, patientsToJsonArray(patients).toString());
        }
    }

    /**
     * Handle POST requests - Create patient
     */
    private void handlePost(HttpExchange exchange) throws IOException {
        try {
            String requestBody = readRequestBody(exchange);
            JSONObject json = new JSONObject(requestBody);
            
            Patient patient = jsonToPatient(json);
            
            // Generate patient ID if not provided
            if (patient.getPatientId() == null || patient.getPatientId().isEmpty()) {
                patient.setPatientId(patientService.generatePatientId());
            }
            
            int patientId = patientService.createPatient(patient);
            
            if (patientId > 0) {
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("message", "Patient created successfully");
                response.put("id", patientId);
                sendJsonResponse(exchange, 201, response.toString());
            } else {
                sendErrorResponse(exchange, 500, "Failed to create patient");
            }
        } catch (JSONException e) {
            sendErrorResponse(exchange, 400, "Invalid JSON: " + e.getMessage());
        }
    }

    /**
     * Handle PUT requests - Update patient
     */
    private void handlePut(HttpExchange exchange, String path) throws IOException {
        try {
            String[] pathParts = path.split("/");
            
            if (pathParts.length <= 3 || pathParts[3].isEmpty()) {
                sendErrorResponse(exchange, 400, "Missing patient ID");
                return;
            }
            
            int patientId = Integer.parseInt(pathParts[3]);
            String requestBody = readRequestBody(exchange);
            JSONObject json = new JSONObject(requestBody);
            
            Patient patient = jsonToPatient(json);
            patient.setId(patientId);
            
            if (patientService.updatePatient(patient)) {
                sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Patient updated successfully\"}");
            } else {
                sendErrorResponse(exchange, 500, "Failed to update patient");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid patient ID");
        } catch (JSONException e) {
            sendErrorResponse(exchange, 400, "Invalid JSON: " + e.getMessage());
        }
    }

    /**
     * Handle DELETE requests - Delete patient
     */
    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        try {
            String[] pathParts = path.split("/");
            
            if (pathParts.length <= 3 || pathParts[3].isEmpty()) {
                sendErrorResponse(exchange, 400, "Missing patient ID");
                return;
            }
            
            int patientId = Integer.parseInt(pathParts[3]);
            
            if (patientService.deletePatient(patientId)) {
                sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Patient deleted successfully\"}");
            } else {
                sendErrorResponse(exchange, 500, "Failed to delete patient");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid patient ID");
        }
    }

    /**
     * Convert Patient to JSON
     */
    private JSONObject patientToJson(Patient patient) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", patient.getId());
        json.put("patientId", patient.getPatientId());
        json.put("patientName", patient.getPatientName());
        json.put("dateOfBirth", patient.getDateOfBirth());
        json.put("age", patient.getAge());
        json.put("gender", patient.getGender());
        json.put("phone", patient.getPhone());
        json.put("email", patient.getEmail());
        json.put("streetAddress", patient.getStreetAddress());
        json.put("city", patient.getCity());
        json.put("state", patient.getState());
        json.put("postalCode", patient.getPostalCode());
        json.put("country", patient.getCountry());
        json.put("referredByDoctorId", patient.getReferredByDoctorId());
        json.put("referralDoctor", patient.getReferralDoctor());
        json.put("medicalHistory", patient.getMedicalHistory());
        json.put("allergies", patient.getAllergies());
        json.put("medications", patient.getMedications());
        json.put("status", patient.getStatus());
        json.put("createdAt", patient.getCreatedAt());
        json.put("updatedAt", patient.getUpdatedAt());
        return json;
    }

    /**
     * Convert JSON to Patient
     */
    private Patient jsonToPatient(JSONObject json) throws JSONException {
        Patient patient = new Patient();
        
        if (json.has("patientId")) patient.setPatientId(json.getString("patientId"));
        if (json.has("patientName")) patient.setPatientName(json.getString("patientName"));
        if (json.has("dateOfBirth")) patient.setDateOfBirth(json.getString("dateOfBirth"));
        if (json.has("age")) patient.setAge(json.getInt("age"));
        if (json.has("gender")) patient.setGender(json.getString("gender"));
        if (json.has("phone")) patient.setPhone(json.getString("phone"));
        if (json.has("email")) patient.setEmail(json.getString("email"));
        if (json.has("streetAddress")) patient.setStreetAddress(json.getString("streetAddress"));
        if (json.has("city")) patient.setCity(json.getString("city"));
        if (json.has("state")) patient.setState(json.getString("state"));
        if (json.has("postalCode")) patient.setPostalCode(json.getString("postalCode"));
        if (json.has("country")) patient.setCountry(json.getString("country"));
        if (json.has("referredByDoctorId")) patient.setReferredByDoctorId(json.getInt("referredByDoctorId"));
        if (json.has("referralDoctor")) patient.setReferralDoctor(json.getString("referralDoctor"));
        if (json.has("medicalHistory")) patient.setMedicalHistory(json.getString("medicalHistory"));
        if (json.has("allergies")) patient.setAllergies(json.getString("allergies"));
        if (json.has("medications")) patient.setMedications(json.getString("medications"));
        if (json.has("status")) patient.setStatus(json.getString("status"));
        
        return patient;
    }

    /**
     * Convert list of patients to JSON array
     */
    private JSONArray patientsToJsonArray(List<Patient> patients) throws JSONException {
        JSONArray array = new JSONArray();
        for (Patient patient : patients) {
            array.put(patientToJson(patient));
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
