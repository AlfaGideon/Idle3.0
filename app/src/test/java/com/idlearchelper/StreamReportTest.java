package com.idlearchelper;

import org.junit.Test;
import static org.junit.Assert.*;

public class StreamReportTest {
    @Test public void safeEventIsReadable() {
        String s=StreamReport.render("{\"kind\":\"ws\",\"stage\":\"message\",\"location\":\"idlearc.com/api/stream/_\",\"shape\":\"json-object:data,event\"}");
        assertTrue(s.contains("WebSocket")); assertTrue(s.contains("json-object:data,event")); assertFalse(s.contains("_"));
    }
    @Test public void secretsAndUnexpectedValuesAreRejected() {
        assertNull(StreamReport.render("{\"kind\":\"ws\",\"stage\":\"open\",\"location\":\"idlearc.com/api/stream?token=secret\"}"));
        assertNull(StreamReport.render("{\"kind\":\"ws\",\"stage\":\"message\",\"location\":\"idlearc.com/_\",\"shape\":\"json-object:password\"}"));
        assertNull(StreamReport.render("{\"kind\":\"ws\",\"stage\":\"secret\",\"location\":\"idlearc.com/_\"}"));
        assertNull(StreamReport.render("{\"kind\":\"ws\",\"stage\":\"open\",\"location\":\"evil.example/ws\"}"));
    }
}
