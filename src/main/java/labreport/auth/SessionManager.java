package labreport.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static class SessionData {
        String username;
        String role;

        SessionData(String username, String role) {
            this.username = username;
            this.role = role;
        }
    }

    private static final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    public static String createSession(String username, String role) {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, new SessionData(username, role));
        return sessionId;
    }

    public static String getUser(String sessionId) {
        SessionData data = sessions.get(sessionId);
        return data != null ? data.username : null;
    }

    public static String getRole(String sessionId) {
        SessionData data = sessions.get(sessionId);
        return data != null ? data.role : null;
    }

    public static Map<String, String> getSessionInfo(String sessionId) {
        SessionData data = sessions.get(sessionId);
        if (data == null) {
            return null;
        }
        Map<String, String> info = new HashMap<>();
        info.put("username", data.username);
        info.put("role", data.role);
        return info;
    }

    public static boolean isValid(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    public static void invalidate(String sessionId) {
        sessions.remove(sessionId);
    }
}
