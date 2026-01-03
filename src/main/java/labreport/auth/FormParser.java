package labreport.auth;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class FormParser {

    public static Map<String, String> parse(String body) throws Exception {
        Map<String, String> map = new HashMap<>();

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=");
            if (parts.length == 2) {
                map.put(
                        URLDecoder.decode(parts[0], "UTF-8"),
                        URLDecoder.decode(parts[1], "UTF-8")
                );
            }
        }
        return map;
    }
}
