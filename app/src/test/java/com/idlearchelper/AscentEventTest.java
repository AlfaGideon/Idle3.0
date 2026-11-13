package com.idlearchelper;
import org.junit.Test;
import static org.junit.Assert.*;
public class AscentEventTest {
    @Test public void verifiedAscentDataIsReadWithoutRounding() {
        AscentEvent e=AscentEvent.parse("{\"kind\":\"ascent\",\"pool\":\"38626\",\"tier\":\"22\",\"overflow\":\"0\",\"contributors\":\"1929\",\"lifetime\":\"18720\"}");
        assertNotNull(e); assertEquals("38626",e.pool); assertEquals("22",e.tier); assertEquals("1929",e.contributors);
        assertNull(AscentEvent.parse("{\"kind\":\"ascent\",\"pool\":\"1\",\"tier\":\"2.5\"}"));
        assertNull(AscentEvent.parse("{\"kind\":\"ascent\",\"pool\":\"not a number\",\"tier\":\"1\"}"));
        assertNull(AscentEvent.parse("{\"kind\":\"other\",\"payload\":\"secret\"}"));
        AscentEvent missing=AscentEvent.parse("{\"kind\":\"ascent\",\"pool\":\"42\",\"tier\":\"2\"}");
        assertNotNull(missing); assertNull(missing.contributors);
    }
    @Test public void statusDoesNotInterpretUntrustedFields() {
        AscentEvent e=AscentEvent.parse("{\"kind\":\"closed\",\"pool\":\"999\"}");
        assertNotNull(e); assertEquals("closed",e.kind); assertNull(e.pool);
    }
}
