package com.idlearchelper;

import org.junit.Test;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class ApiLoginTest {
    static class FakeConnection extends HttpURLConnection {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        int status; String response;
        FakeConnection(int status, String response) throws Exception {
            super(new URL("https://idlearc.com/api/login")); this.status=status; this.response=response;
        }
        @Override public void connect() { }
        @Override public void disconnect() { }
        @Override public boolean usingProxy() { return false; }
        @Override public OutputStream getOutputStream() { return body; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)); }
        @Override public int getResponseCode() { return status; }
        @Override public String getContentType() { return "application/json"; }
    }
    @Test public void apiLoginAndBearerContract() throws Exception {
        DiagnosticLog.clear();
        FakeConnection c = new FakeConnection(200, "{\"token\":\"secret-token\"}");
        assertEquals("secret-token", Api.loginWithConnection(c, "user@example.test", "secret-password"));
        assertEquals("POST", c.getRequestMethod());
        assertFalse(c.getInstanceFollowRedirects());
        assertTrue(c.getRequestProperty("Content-Type").startsWith("application/json"));
        JSONObject request = new JSONObject(c.body.toString("UTF-8"));
        assertEquals("user@example.test", request.getString("email"));
        assertEquals("secret-password", request.getString("password"));
        assertEquals("IdleArc Helper", request.getString("device_name"));
        assertFalse(DiagnosticLog.snapshot().contains("secret"));
        FakeConnection dashboard = new FakeConnection(200, "{}");
        Api.applyBearer(dashboard, "secret-token");
        assertEquals("Bearer secret-token", dashboard.getRequestProperty("Authorization"));
    }
    @Test public void noTokenOrUnauthorizedIsNotSuccess() throws Exception {
        for (FakeConnection c : new FakeConnection[]{new FakeConnection(401, "{}"), new FakeConnection(200, "{}"), new FakeConnection(302, "{}")}) {
            try { Api.loginWithConnection(c, "x", "y"); fail("must fail"); } catch (IOException expected) { }
        }
    }
}
