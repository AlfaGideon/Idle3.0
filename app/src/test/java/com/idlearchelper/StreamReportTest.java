package com.idlearchelper;

import org.junit.Test;
import static org.junit.Assert.*;

public class StreamReportTest {
    @Test public void safeEventIsReadable() {
        String s=StreamReport.render("{\"kind\":\"ws\",\"stage\":\"message\",\"location\":\"idlearc.com/api/stream/_\",\"shape\":\"json-object:data,event\"}");
        assertTrue(s.contains("WebSocket")); assertTrue(s.contains("json-object:data,event")); assertFalse(s.contains("_"));
    }
    @Test public void protocolMetadataIsLimitedToSafeCategories() {
        String event=StreamReport.render("{\"kind\":\"ws\",\"stage\":\"sent\",\"location\":\"ws-eu.pusher.com/app/_\",\"shape\":\"json-object:data,event\",\"meta\":\"subscribe|private|chat\"}");
        assertNotNull(event); assertTrue(event.contains("ws-eu.pusher.com"));
        assertTrue(event.contains("subscribe")); assertTrue(event.contains("private"));
        assertNull(StreamReport.render("{\"kind\":\"ws\",\"stage\":\"message\",\"location\":\"idlearc.com/_\",\"shape\":\"json-object\",\"meta\":\"secret|private|chat\"}"));
        assertNull(StreamReport.render("{\"kind\":\"ws\",\"stage\":\"open\",\"location\":\"ws-user-secret.pusher.com/app/_\"}"));
    }
    @Test public void secretsAndUnexpectedValuesAreRejected() {
        assertNull(StreamReport.render("{\"kind\":\"ws\",\"stage\":\"open\",\"location\":\"idlearc.com/api/stream?token=secret\"}"));
        assertNull(StreamReport.render("{\"kind\":\"ws\",\"stage\":\"message\",\"location\":\"idlearc.com/_\",\"shape\":\"json-object:password\"}"));
        assertNull(StreamReport.render("{\"kind\":\"ws\",\"stage\":\"secret\",\"location\":\"idlearc.com/_\"}"));
        assertNull(StreamReport.render("{\"kind\":\"ws\",\"stage\":\"open\",\"location\":\"evil.example/ws\"}"));
    }
}
