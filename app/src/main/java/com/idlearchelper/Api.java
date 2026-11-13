package com.idlearchelper;

import android.webkit.CookieManager;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Read-only client. The WebView owns the session; only cookies for the exact site are sent. */
final class Api {
    static final String BASE = "https://idlearc.com";
    static Object get(String path) throws Exception {
        if (!path.equals("/api/dashboard") && !path.equals("/api/config/game")) throw new IOException("Недопустимый адрес");
        HttpURLConnection c = (HttpURLConnection) new URL(BASE + path).openConnection();
        c.setInstanceFollowRedirects(false);
        c.setConnectTimeout(15000); c.setReadTimeout(20000);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("User-Agent", android.webkit.WebSettings.getDefaultUserAgent(App.context));
        String cookie = CookieManager.getInstance().getCookie(BASE + path);
        if (cookie != null) c.setRequestProperty("Cookie", cookie);
        DiagnosticLog.add("API GET " + path + ": cookie для запроса " + (cookie == null || cookie.isEmpty() ? "нет" : "есть") + ", автоматические редиректы выключены");
        try {
            int status = c.getResponseCode();
            String contentType = c.getContentType();
            DiagnosticLog.add("API " + path + ": HTTP " + status + ", тип " + DiagnosticLog.contentType(contentType));
            if (status >= 300 && status < 400) {
                String location = c.getHeaderField("Location");
                if (location != null) {
                    try { location = new URL(new URL(BASE + path), location).toString(); } catch (Exception ignored) { location = null; }
                }
                DiagnosticLog.add("API редирект → " + DiagnosticLog.location(location));
            }
            if (status == 401 || status == 403 || (status >= 300 && status < 400))
                throw new IOException("Требуется вход или сессия истекла. Войдите снова.");
            if (status != 200) throw new IOException("Сервер вернул HTTP " + status);
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).contains("json"))
                throw new IOException("Сервер вернул не JSON. Проверьте вход и доступ к IdleArc.");
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int n; int total = 0;
                while ((n = in.read(buffer)) != -1) { total += n; if (total > 12_000_000) throw new IOException("Ответ слишком большой"); out.write(buffer, 0, n); }
                String text = new String(out.toByteArray(), StandardCharsets.UTF_8);
                Object parsed = new JSONTokener(text).nextValue();
                DiagnosticLog.add("API " + path + ": JSON получен (" + total + " байт)");
                return parsed;
            }
        } catch (Exception e) {
            DiagnosticLog.add("API " + path + ": " + DiagnosticLog.errorType(e));
            throw e;
        } finally { c.disconnect(); }
    }
    static Object at(Object root, JSONArray path) {
        Object node = root;
        try {
            for (int i = 0; i < path.length(); i++) {
                Object key = path.get(i);
                if (node instanceof JSONObject && key instanceof String) node = ((JSONObject)node).get((String) key);
                else if (node instanceof JSONArray && key instanceof Number) node = ((JSONArray)node).get(((Number)key).intValue());
                else return null;
            }
            return node;
        } catch (JSONException e) { return null; }
    }
    static String label(JSONArray path) {
        StringBuilder s = new StringBuilder();
        for (int i=0;i<path.length();i++) { if (i>0) s.append(" / "); s.append(path.opt(i)); }
        return s.toString();
    }
    static String value(Object o) { return o == null || o == JSONObject.NULL ? "—" : String.valueOf(o); }
}
