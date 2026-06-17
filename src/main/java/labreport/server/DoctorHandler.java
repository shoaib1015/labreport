package labreport.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import labreport.auth.FormParser;
import labreport.auth.ReferringDoctorService;
import labreport.logging.AppLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

public class DoctorHandler implements HttpHandler {

    private static final Logger log = AppLogger.getLogger();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method) && path.endsWith("/api/doctors")) {
                handleGetAllDoctors(exchange);
            } else if ("GET".equals(method) && path.matches(".*/api/doctors/\\d+")) {
                int doctorId = extractId(path);
                handleGetDoctorById(exchange, doctorId);
            } else if ("POST".equals(method) && path.endsWith("/api/doctors")) {
                handleAddDoctor(exchange);
            } else if ("POST".equals(method) && path.matches(".*/api/doctors/\\d+")) {
                int doctorId = extractId(path);
                handleUpdateDoctor(exchange, doctorId);
            } else if ("DELETE".equals(method) && path.matches(".*/api/doctors/\\d+")) {
                int doctorId = extractId(path);
                handleDeleteDoctor(exchange, doctorId);
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
            }

        } catch (Exception e) {
            log.severe("Doctor handler error: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleGetAllDoctors(HttpExchange exchange) throws IOException {
        try {
            List<Map<String, String>> doctors = ReferringDoctorService.getAllDoctors();

            String response = listToJson(doctors);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }

            log.info("Doctors fetched successfully, count: " + doctors.size());

        } catch (Exception e) {
            log.severe("Failed to get doctors: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleGetDoctorById(HttpExchange exchange, int doctorId) throws IOException {
        try {
            Map<String, String> doctor = ReferringDoctorService.getDoctorById(doctorId);

            String response = toJson(doctor);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }

            log.info("Doctor fetched successfully: id=" + doctorId);

        } catch (Exception e) {
            log.severe("Failed to get doctor by id: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private void handleAddDoctor(HttpExchange exchange) throws IOException {
        try {
            String body = readBody(exchange.getRequestBody());
            Map<String, String> params = FormParser.parse(body);

            String fullName = params.get("full_name");
            String contactNumber = params.get("contact_number");
            String licenseNumber = params.get("license_number");
            String status = params.get("status");
            double commissionPercent = parseDouble(params.get("commission_percent"), 0.0);

            if (fullName == null || fullName.trim().isEmpty()) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, -1);
                return;
            }

            boolean success = ReferringDoctorService.addDoctor(fullName, contactNumber, licenseNumber, status, commissionPercent);

            if (success) {
                String response = "{\"status\": \"success\", \"message\": \"Doctor added successfully\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }

                log.info("Doctor added successfully");
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            }

        } catch (Exception e) {
            log.severe("Failed to add doctor: " + e.getMessage());
            handleDoctorSaveException(exchange, e, "Failed to add doctor");
        }
    }

    private void handleUpdateDoctor(HttpExchange exchange, int doctorId) throws IOException {
        try {
            String body = readBody(exchange.getRequestBody());
            Map<String, String> params = FormParser.parse(body);

            String fullName = params.get("full_name");
            String contactNumber = params.get("contact_number");
            String licenseNumber = params.get("license_number");
            String status = params.get("status");
            double commissionPercent = parseDouble(params.get("commission_percent"), 0.0);

            if (fullName == null || fullName.trim().isEmpty()) {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, -1);
                return;
            }

            boolean success = ReferringDoctorService.updateDoctor(doctorId, fullName, contactNumber, licenseNumber, status, commissionPercent);

            if (success) {
                String response = "{\"status\": \"success\", \"message\": \"Doctor updated successfully\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }

                log.info("Doctor updated successfully");
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            }

        } catch (Exception e) {
            log.severe("Failed to update doctor: " + e.getMessage());
            handleDoctorSaveException(exchange, e, "Failed to update doctor");
        }
    }

    private void handleDeleteDoctor(HttpExchange exchange, int doctorId) throws IOException {
        try {
            boolean success = ReferringDoctorService.deleteDoctor(doctorId);

            if (success) {
                String response = "{\"status\": \"success\", \"message\": \"Doctor deleted successfully\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }

                log.info("Doctor deleted successfully");
            } else {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            }

        } catch (Exception e) {
            log.severe("Failed to delete doctor: " + e.getMessage());
            exchange.sendResponseHeaders(500, -1);
        }
    }

    private int extractId(String path) {
        String[] parts = path.split("/");
        return Integer.parseInt(parts[parts.length - 1]);
    }

    private String readBody(InputStream is) {
        Scanner s = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private double parseDouble(String value, double defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String listToJson(List<Map<String, String>> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String toJson(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":\"")
                    .append(escapeJson(entry.getValue())).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private void handleDoctorSaveException(HttpExchange exchange, Exception e, String defaultMessage) throws IOException {
        String errorMessage = defaultMessage;
        int statusCode = HttpURLConnection.HTTP_INTERNAL_ERROR;

        if (isDuplicateLicenseException(e)) {
            errorMessage = "License number already exists. Please use a different license number.";
            statusCode = HttpURLConnection.HTTP_CONFLICT;
        }

        String errorResponse = "{\"error\": \"duplicate_license\", \"message\": \"" + escapeJson(errorMessage) + "\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, errorResponse.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(errorResponse.getBytes(StandardCharsets.UTF_8));
        }
    }

    private boolean isDuplicateLicenseException(Throwable e) {
        if (e == null) {
            return false;
        }
        if (e instanceof SQLException) {
            String message = e.getMessage();
            if (message != null && message.contains("UNIQUE constraint failed") && message.contains("license_number")) {
                return true;
            }
        }
        return isDuplicateLicenseException(e.getCause());
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
