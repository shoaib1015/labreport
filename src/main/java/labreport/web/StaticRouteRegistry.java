package labreport.web;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpContext;
import labreport.server.AuthFilter;
import labreport.server.CorsFilter;
import labreport.server.StaticFileHandler;


import java.util.Arrays;
import java.util.List;

public final class StaticRouteRegistry {

    private StaticRouteRegistry() {
        // prevent instantiation
    }

    public static void register(HttpServer server) {

        // 1️⃣ Public routes (no authentication)
        registerPublic(server, Arrays.asList(
                "/app.html",
                "/login.html",
                "/styles.css"
        ));

        // 2️⃣ Protected routes (authentication required)
        registerProtected(server, Arrays.asList(
                "/dashboard.html",

                // Masters
                "/master/lab.html",
                "/master/test-list.html",
                "/master/doctor.html",

                // Patients
                "/patient/patient-edit.html",
                "/patient/patient-list.html",

                // Reports
                "/report/preview.html",
                "/report/print.html"
        ));
    }

    /* ---------------- INTERNAL HELPERS ---------------- */

    private static void registerPublic(HttpServer server, List<String> routes) {
        for (String route : routes) {
            HttpContext ctx = server.createContext(
                    route,
                    new StaticFileHandler("/web" + route)
            );
            ctx.getFilters().add(new CorsFilter());
        }
    }

    private static void registerProtected(HttpServer server, List<String> routes) {
        for (String route : routes) {
            HttpContext ctx = server.createContext(
                    route,
                    new StaticFileHandler("/web" + route)
            );
            ctx.getFilters().add(new CorsFilter());
            ctx.getFilters().add(new AuthFilter());
        }
    }
}
