package pt.ulisboa.tecnico.cnv.loadbalancer.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

public final class HttpUtils {

    public static Map<String, String> queryToMap(String query) {
        if (query == null) {
            return null;
        }
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }

    public static void sendError(HttpExchange he, int errorCode, String message) throws IOException {
        String errorResponse = "{ \"error\":\"" + message + "\"}";
        sendResponse(he, errorCode, errorResponse);
    }

    public static void sendResponse(HttpExchange he, int code, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(code, bytes.length);
        OutputStream os = he.getResponseBody();
        os.write(bytes);
    }

}
