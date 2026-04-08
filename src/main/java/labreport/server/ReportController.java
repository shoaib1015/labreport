package labreport.server;

import labreport.model.Report;
import labreport.model.ReportTestResult;
import labreport.service.ReportService;
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
 * REST API Controller for Report Management
 * Endpoints:
 * - GET /api/reports - Get all reports
 * - GET /api/reports?patientId=id - Get reports for patient
 * - GET /api/reports?doctorId=id - Get reports for doctor
 * - GET /api/reports?startDate=date&endDate=date - Get reports by date range
 * - GET /api/reports/{id} - Get report with test results
 * - POST /api/reports - Create report
 * - PUT /api/reports/{id} - Update report
 * - DELETE /api/reports/{id} - Delete report
 * - POST /api/reports/{id}/results - Add test result
 * - PUT /api/reports/{reportId}/results/{resultId} - Update test result
 * - DELETE /api/reports/{reportId}/results/{resultId} - Delete test result
 */
public class ReportController implements HttpHandler {
    private ReportService reportService;

    public ReportController(DatabaseManager dbManager) {
        this.reportService = new ReportService(dbManager);
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
        String[] pathParts = path.split("/");
        
        if (pathParts.length > 3 && !pathParts[3].isEmpty() && !pathParts[3].contains("results")) {
            // GET /api/reports/{id}
            try {
                int reportId = Integer.parseInt(pathParts[3]);
                Report report = reportService.getReportById(reportId);
                
                if (report != null) {
                    sendJsonResponse(exchange, 200, reportToJson(report).toString());
                } else {
                    sendErrorResponse(exchange, 404, "Report not found");
                }
            } catch (NumberFormatException e) {
                sendErrorResponse(exchange, 400, "Invalid report ID");
            }
        } else if (query != null) {
            if (query.contains("patientId=")) {
                // GET /api/reports?patientId=id
                try {
                    String patientIdStr = query.split("patientId=")[1];
                    int patientId = Integer.parseInt(patientIdStr);
                    
                    List<Report> reports = reportService.getReportsByPatientId(patientId);
                    sendJsonResponse(exchange, 200, reportsToJsonArray(reports).toString());
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid patient ID");
                }
            } else if (query.contains("doctorId=")) {
                // GET /api/reports?doctorId=id
                try {
                    String doctorIdStr = query.split("doctorId=")[1];
                    int doctorId = Integer.parseInt(doctorIdStr);
                    
                    List<Report> reports = reportService.getReportsByDoctorId(doctorId);
                    sendJsonResponse(exchange, 200, reportsToJsonArray(reports).toString());
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid doctor ID");
                }
            } else if (query.contains("startDate=")) {
                // GET /api/reports?startDate=date&endDate=date
                try {
                    String[] parts = query.split("&");
                    String startDate = parts[0].split("startDate=")[1];
                    String endDate = parts.length > 1 ? parts[1].split("endDate=")[1] : startDate;
                    
                    startDate = java.net.URLDecoder.decode(startDate, StandardCharsets.UTF_8.name());
                    endDate = java.net.URLDecoder.decode(endDate, StandardCharsets.UTF_8.name());
                    
                    List<Report> reports = reportService.getReportsByDateRange(startDate, endDate);
                    sendJsonResponse(exchange, 200, reportsToJsonArray(reports).toString());
                } catch (Exception e) {
                    sendErrorResponse(exchange, 400, "Invalid date parameters");
                }
            } else {
                // Fallback: Get all reports
                List<Report> reports = reportService.getAllReports();
                sendJsonResponse(exchange, 200, reportsToJsonArray(reports).toString());
            }
        } else {
            // GET /api/reports - Get all reports
            List<Report> reports = reportService.getAllReports();
            sendJsonResponse(exchange, 200, reportsToJsonArray(reports).toString());
        }
    }

    /**
     * Handle POST requests
     */
    private void handlePost(HttpExchange exchange, String path) throws IOException {
        try {
            String requestBody = readRequestBody(exchange);
            JSONObject json = new JSONObject(requestBody);
            String[] pathParts = path.split("/");
            
            if (path.contains("/results")) {
                // POST /api/reports/{id}/results - Add test result
                int reportId = Integer.parseInt(pathParts[3]);
                
                ReportTestResult testResult = jsonToTestResult(json);
                testResult.setReportId(reportId);
                
                int resultId = reportService.addTestResult(testResult);
                if (resultId > 0) {
                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Test result added successfully");
                    response.put("id", resultId);
                    sendJsonResponse(exchange, 201, response.toString());
                } else {
                    sendErrorResponse(exchange, 500, "Failed to add test result");
                }
            } else {
                // POST /api/reports - Create report
                Report report = jsonToReport(json);
                int reportId = reportService.createReport(report);
                
                if (reportId > 0) {
                    JSONObject response = new JSONObject();
                    response.put("success", true);
                    response.put("message", "Report created successfully");
                    response.put("id", reportId);
                    sendJsonResponse(exchange, 201, response.toString());
                } else {
                    sendErrorResponse(exchange, 500, "Failed to create report");
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
            
            if (path.contains("/results/")) {
                // PUT /api/reports/{reportId}/results/{resultId}
                int resultId = Integer.parseInt(pathParts[5]);
                
                ReportTestResult testResult = jsonToTestResult(json);
                testResult.setId(resultId);
                
                if (reportService.updateTestResult(testResult)) {
                    sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Test result updated successfully\"}");
                } else {
                    sendErrorResponse(exchange, 500, "Failed to update test result");
                }
            } else {
                // PUT /api/reports/{id}
                int reportId = Integer.parseInt(pathParts[3]);
                
                Report report = jsonToReport(json);
                report.setId(reportId);
                
                if (reportService.updateReport(report)) {
                    sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Report updated successfully\"}");
                } else {
                    sendErrorResponse(exchange, 500, "Failed to update report");
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
            
            if (path.contains("/results/")) {
                // DELETE /api/reports/{reportId}/results/{resultId}
                int resultId = Integer.parseInt(pathParts[5]);
                
                if (reportService.deleteTestResult(resultId)) {
                    sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Test result deleted successfully\"}");
                } else {
                    sendErrorResponse(exchange, 500, "Failed to delete test result");
                }
            } else {
                // DELETE /api/reports/{id}
                int reportId = Integer.parseInt(pathParts[3]);
                
                if (reportService.deleteReport(reportId)) {
                    sendJsonResponse(exchange, 200, "{\"success\": true, \"message\": \"Report deleted successfully\"}");
                } else {
                    sendErrorResponse(exchange, 500, "Failed to delete report");
                }
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(exchange, 400, "Invalid ID format");
        }
    }

    // ============ JSON CONVERSION HELPERS ============

    private JSONObject reportToJson(Report report) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", report.getId());
        json.put("patientId", report.getPatientId());
        json.put("reportDate", report.getReportDate());
        json.put("referredByDoctorId", report.getReferredByDoctorId());
        json.put("clinicalNotes", report.getClinicalNotes());
        json.put("sampleCollectionDate", report.getSampleCollectionDate());
        json.put("reportSubmittedDate", report.getReportSubmittedDate());
        json.put("testingBy", report.getTestingBy());
        json.put("approvedBy", report.getApprovedBy());
        json.put("status", report.getStatus());
        json.put("filePath", report.getFilePath());
        json.put("testResults", testResultsToJsonArray(report.getTestResults()));
        json.put("createdAt", report.getCreatedAt());
        json.put("updatedAt", report.getUpdatedAt());
        return json;
    }

    private Report jsonToReport(JSONObject json) throws JSONException {
        Report report = new Report();
        if (json.has("patientId")) report.setPatientId(json.getInt("patientId"));
        if (json.has("reportDate")) report.setReportDate(json.getString("reportDate"));
        if (json.has("referredByDoctorId")) report.setReferredByDoctorId(json.getInt("referredByDoctorId"));
        if (json.has("clinicalNotes")) report.setClinicalNotes(json.getString("clinicalNotes"));
        if (json.has("sampleCollectionDate")) report.setSampleCollectionDate(json.getString("sampleCollectionDate"));
        if (json.has("reportSubmittedDate")) report.setReportSubmittedDate(json.getString("reportSubmittedDate"));
        if (json.has("testingBy")) report.setTestingBy(json.getString("testingBy"));
        if (json.has("approvedBy")) report.setApprovedBy(json.getString("approvedBy"));
        if (json.has("status")) report.setStatus(json.getString("status"));
        if (json.has("filePath")) report.setFilePath(json.getString("filePath"));
        return report;
    }

    private JSONObject testResultToJson(ReportTestResult result) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", result.getId());
        json.put("reportId", result.getReportId());
        json.put("testId", result.getTestId());
        json.put("subTestId", result.getSubTestId());
        json.put("testName", result.getTestName());
        json.put("subTestName", result.getSubTestName());
        json.put("resultValue", result.getResultValue());
        json.put("unit", result.getUnit());
        json.put("normalRangeMin", result.getNormalRangeMin());
        json.put("normalRangeMax", result.getNormalRangeMax());
        json.put("isAbnormal", result.getIsAbnormal());
        json.put("notes", result.getNotes());
        json.put("createdAt", result.getCreatedAt());
        json.put("updatedAt", result.getUpdatedAt());
        return json;
    }

    private ReportTestResult jsonToTestResult(JSONObject json) throws JSONException {
        ReportTestResult result = new ReportTestResult();
        if (json.has("testId")) result.setTestId(json.getInt("testId"));
        if (json.has("subTestId")) result.setSubTestId(json.getInt("subTestId"));
        if (json.has("testName")) result.setTestName(json.getString("testName"));
        if (json.has("subTestName")) result.setSubTestName(json.getString("subTestName"));
        if (json.has("resultValue")) result.setResultValue(json.getString("resultValue"));
        if (json.has("unit")) result.setUnit(json.getString("unit"));
        if (json.has("normalRangeMin")) result.setNormalRangeMin(json.getDouble("normalRangeMin"));
        if (json.has("normalRangeMax")) result.setNormalRangeMax(json.getDouble("normalRangeMax"));
        if (json.has("isAbnormal")) result.setIsAbnormal(json.getBoolean("isAbnormal"));
        if (json.has("notes")) result.setNotes(json.getString("notes"));
        return result;
    }

    private JSONArray reportsToJsonArray(List<Report> reports) throws JSONException {
        JSONArray array = new JSONArray();
        for (Report report : reports) {
            array.put(reportToJson(report));
        }
        return array;
    }

    private JSONArray testResultsToJsonArray(List<ReportTestResult> results) throws JSONException {
        JSONArray array = new JSONArray();
        for (ReportTestResult result : results) {
            array.put(testResultToJson(result));
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
