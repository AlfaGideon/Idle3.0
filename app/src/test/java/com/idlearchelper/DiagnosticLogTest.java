package com.idlearchelper;

import org.junit.Test;
import static org.junit.Assert.*;

public class DiagnosticLogTest {
    @Test public void urlIsReducedToSafeLabels() {
        assertEquals("idlearc.com/login", DiagnosticLog.location("https://idlearc.com/login?token=secret#password"));
        assertEquals("www.idlearc.com/другая-страница", DiagnosticLog.location("https://www.idlearc.com/private/secret"));
        assertEquals("внешний адрес", DiagnosticLog.location("https://idlearc.com.evil.example/login?cookie=secret"));
        assertEquals("idlearc.com/app/…", DiagnosticLog.location("https://idlearc.com/app/user/secret"));
        assertEquals("нет адреса", DiagnosticLog.location(null));
    }
    @Test public void responseHeadersAreNotCopied() {
        assertEquals("JSON", DiagnosticLog.contentType("application/json; secret=token"));
        assertEquals("HTML", DiagnosticLog.contentType("text/html; secret=token"));
        assertEquals("другой", DiagnosticLog.contentType("secret=token"));
    }
    @Test public void logHasBoundedLifetimeAndSize() {
        DiagnosticLog.clear();
        for (int i=0; i<205; i++) DiagnosticLog.add("test " + i);
        String log = DiagnosticLog.snapshot();
        assertFalse(log.contains("test 0\n"));
        assertTrue(log.contains("test 5\n"));
        assertTrue(log.contains("test 204\n"));
        DiagnosticLog.clear();
        assertFalse(DiagnosticLog.snapshot().contains("test 204"));
    }
}
