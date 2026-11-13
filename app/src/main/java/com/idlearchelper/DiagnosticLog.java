package com.idlearchelper;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

/** Only callers' explicitly selected metadata is recorded; never log URLs, cookies or response bodies. */
final class DiagnosticLog {
    private static final int MAX_ENTRIES = 200;
    private static final ArrayDeque<String> entries = new ArrayDeque<>();
    static synchronized void add(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(new Date());
        if (entries.size() == MAX_ENTRIES) entries.removeFirst();
        entries.addLast(timestamp + "  " + message);
    }
    static synchronized String snapshot() {
        if (entries.isEmpty()) return "Пока нет событий. Попробуйте войти и проверить данные.";
        StringBuilder b = new StringBuilder();
        for (String line : entries) b.append(line).append('\n');
        return b.toString();
    }
    static synchronized void clear() { entries.clear(); }

    // Reduces arbitrary URLs to an allowlisted label. Query strings, fragments, external hosts
    // and unknown paths may contain credentials and are deliberately never returned.
    static String location(String url) {
        if (url == null) return "нет адреса";
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null || !(host.equalsIgnoreCase("idlearc.com") || host.equalsIgnoreCase("www.idlearc.com"))) return "внешний адрес";
            String origin = host.equalsIgnoreCase("www.idlearc.com") ? "www.idlearc.com" : "idlearc.com";
            String path = uri.getPath();
            String category;
            if ("/login".equals(path) || "/login/".equals(path)) category = "/login";
            else if ("/api/dashboard".equals(path)) category = "/api/dashboard";
            else if ("/api/config/game".equals(path)) category = "/api/config/game";
            else if ("/app".equals(path) || (path != null && path.startsWith("/app/"))) category = "/app/…";
            else if ("/".equals(path) || path == null || path.isEmpty()) category = "/";
            else category = "/другая-страница";
            return origin + category;
        } catch (Exception e) { return "некорректный адрес"; }
    }
    static String contentType(String type) {
        if (type == null) return "не указан";
        String lower = type.toLowerCase(Locale.ROOT);
        if (lower.contains("json")) return "JSON";
        if (lower.contains("html")) return "HTML";
        return "другой";
    }
    static String errorType(Exception e) {
        // Class names supplied by the platform are safe; exception messages can contain URLs or data.
        if (e instanceof java.net.SocketTimeoutException) return "тайм-аут";
        if (e instanceof java.net.UnknownHostException) return "DNS";
        if (e instanceof javax.net.ssl.SSLException) return "TLS";
        if (e instanceof java.net.ConnectException) return "соединение";
        if (e instanceof java.io.IOException) return "ввод/вывод или HTTP";
        if (e instanceof org.json.JSONException) return "ошибка JSON";
        return "другая ошибка";
    }
}
