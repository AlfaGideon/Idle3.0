package com.idlearchelper;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** HTTPS API client. Credentials are used only by /api/login; dashboard uses a Bearer token. */
final class Api {
    static final String BASE = "https://idlearc.com";
    static Object get(String path) throws Exception {
        if (!path.equals("/api/dashboard") && !path.equals("/api/config/game")) throw new IOException("Недопустимый адрес");
        HttpURLConnection c = (HttpURLConnection) new URL(BASE + path).openConnection();
        c.setInstanceFollowRedirects(false);
        c.setConnectTimeout(15000); c.setReadTimeout(20000);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("User-Agent", "IdleArcHelper/1.2");
        if (path.equals("/api/dashboard")) {
            String token;
            try { token = AuthStore.load(); } catch (Exception e) {
                DiagnosticLog.add("API dashboard: не удалось открыть защищённую сессию");
                throw new IOException("Не удалось открыть сессию. Войдите заново.");
            }
            DiagnosticLog.add("API GET /api/dashboard: Bearer-токен " + (token == null || token.isEmpty() ? "отсутствует" : "есть"));
            if (token == null || token.isEmpty()) throw new IOException("Сначала войдите в аккаунт IdleArc.");
            applyBearer(c, token);
        } else DiagnosticLog.add("API GET /api/config/game: открытый запрос");
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
    static void applyBearer(HttpURLConnection c, String token) { c.setRequestProperty("Authorization", "Bearer " + token); }
    static String login(String email, String password) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(BASE + "/api/login").openConnection();
        return loginWithConnection(c, email, password);
    }
    static String loginWithConnection(HttpURLConnection c, String email, String password) throws Exception {
        c.setInstanceFollowRedirects(false);
        c.setConnectTimeout(15000); c.setReadTimeout(20000);
        c.setRequestMethod("POST"); c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("User-Agent", "IdleArcHelper/1.2");
        DiagnosticLog.add("API POST /api/login: начало входа");
        try {
            JSONObject body = new JSONObject();
            body.put("email", email); body.put("password", password); body.put("device_name", "IdleArc Helper");
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(payload.length);
            try (OutputStream out = c.getOutputStream()) { out.write(payload); }
            int status = c.getResponseCode();
            DiagnosticLog.add("API /api/login: HTTP " + status + ", тип " + DiagnosticLog.contentType(c.getContentType()));
            if (status != 200 && status != 201) {
                if (status == 401 || status == 422) throw new IOException("Не удалось войти (HTTP " + status + "). Проверьте email и пароль.");
                throw new IOException("Ошибка входа: HTTP " + status + ". Попробуйте позже.");
            }
            byte[] buffer = new byte[4096]; int count; int size = 0;
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream in = c.getInputStream()) {
                while ((count = in.read(buffer)) != -1) {
                    size += count; if (size > 1_000_000) throw new IOException("Ответ входа слишком большой.");
                    bytes.write(buffer, 0, count);
                }
            }
            JSONObject result = new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            String token = result.optString("token", "").trim();
            if (token.isEmpty() || token.length() > 8192 || token.indexOf('\n') >= 0 || token.indexOf('\r') >= 0)
                throw new IOException("Сервер не вернул корректный токен.");
            DiagnosticLog.add("API /api/login: токен получен");
            return token;
        } catch (Exception e) {
            DiagnosticLog.add("API /api/login: " + DiagnosticLog.errorType(e));
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
