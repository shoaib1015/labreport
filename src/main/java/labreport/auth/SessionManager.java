package labreport.auth;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static final Map<String, String> sessions = new ConcurrentHashMap<>();

    public static String createSession(String username) {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, username);
        return sessionId;
    }

    public static String getUser(String sessionId) {
        return sessions.get(sessionId);
    }

    public static boolean isValid(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    public static void invalidate(String sessionId) {
        sessions.remove(sessionId);
    }
}
