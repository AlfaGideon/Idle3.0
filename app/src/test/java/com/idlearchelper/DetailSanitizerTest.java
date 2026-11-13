package com.idlearchelper;

import org.junit.Test;
import org.json.JSONObject;
import static org.junit.Assert.*;

public class DetailSanitizerTest {
    @Test public void masksSecretsAndPreservesUsefulEvent() throws Exception {
        String payload="{\"event\":\"CharacterChanged\",\"channel\":\"private-character.42\",\"data\":\"{\\\"level\\\":5,\\\"token\\\":\\\"secret-value\\\",\\\"password\\\":\\\"password-value\\\"}\",\"auth\":\"auth-value\",\"text\":\"Bearer bearer-value email me@test.example\"}";
        String clean=DetailSanitizer.clean(payload);
        assertTrue(clean.contains("CharacterChanged")); assertTrue(clean.contains("level"));
        assertFalse(clean.contains("secret-value")); assertFalse(clean.contains("auth-value"));
        assertFalse(clean.contains("password-value")); assertFalse(clean.contains("bearer-value")); assertFalse(clean.contains("me@test.example"));
        assertEquals(5, new JSONObject(clean).getJSONObject("data").getInt("level"));
    }
    @Test public void detailedOutputIsOptInAndNeverIncludesOutgoingPayload() throws Exception {
        JSONObject message=new JSONObject().put("kind","ws").put("stage","message").put("location","external/app/_")
            .put("shape","json-object:data,event").put("meta","custom|private|other|")
            .put("payload","{\"event\":\"GameUpdate\",\"token\":\"topsecret\",\"data\":{\"level\":5}}");
        String safe=StreamReport.render(message.toString());
        String detailed=StreamReport.render(message.toString(),true);
        assertFalse(safe.contains("GameUpdate"));
        assertTrue(detailed.contains("GameUpdate")); assertTrue(detailed.contains("level")); assertFalse(detailed.contains("topsecret"));
        message.put("stage","sent"); assertFalse(StreamReport.render(message.toString(),true).contains("GameUpdate"));
    }
    @Test public void hostIsValidatedAndOnlyInDetailedMode() throws Exception {
        JSONObject message=new JSONObject().put("kind","ws").put("stage","open").put("location","external/app/_").put("host","socket.example.com:443");
        assertFalse(StreamReport.render(message.toString()).contains("socket.example.com"));
        assertTrue(StreamReport.render(message.toString(),true).contains("socket.example.com:443"));
        message.put("host","socket.example.com?token=secret");
        assertFalse(StreamReport.render(message.toString(),true).contains("secret"));
    }
}
