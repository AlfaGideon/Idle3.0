package com.idlearchelper;

import org.json.*;
import java.util.*;
import java.util.regex.Pattern;

/** Best-effort scrubbing for a user-reviewed diagnostic report. Never use for authorization traffic. */
final class DetailSanitizer {
    private static final String HIDDEN="[скрыто]";
    private static final Pattern SECRET_KEY=Pattern.compile("(?i)(pass(word)?|passwd|token|secret|auth|cookie|session|credential|signature|api.?key|private.?key|socket.?id|csrf|xsrf)");
    private static final Pattern BEARER=Pattern.compile("(?i)bearer\\s+[^\\s,;\\\"]+");
    private static final Pattern JWT=Pattern.compile("\\beyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\b");
    private static final Pattern ASSIGNMENT=Pattern.compile("(?i)(password|token|secret|authorization|cookie|auth|signature|api[_-]?key|session)\\s*[:=]\\s*[^\\s,;]+" );
    private static final Pattern URL_QUERY=Pattern.compile("(https?://[^\\s?#]+)\\?[^\\s#]*");
    private static final Pattern EMAIL=Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    static String clean(String raw) {
        if(raw==null || raw.length()>20000) return "[сообщение отсутствует или слишком большое]";
        try {
            Object value=new JSONTokener(raw).nextValue();
            Object clean=scrub(value,0);
            String text=clean instanceof String ? (String) clean : String.valueOf(clean);
            return text.length()>5000 ? text.substring(0,5000)+"… [сокращено]" : text;
        } catch(Exception e) { return cleanText(raw); }
    }
    private static Object scrub(Object value,int depth) throws JSONException {
        if(depth>=5) return "[глубина ограничена]";
        if(value instanceof JSONObject) {
            JSONObject source=(JSONObject)value, out=new JSONObject(); Iterator<String> keys=source.keys(); int n=0;
            while(keys.hasNext()) {
                String key=keys.next(); if(++n>40) { out.put("_ограничение", "другие поля скрыты"); break; }
                if(key.length()>80 || key.contains("@") || key.contains("?") || key.contains("/")) { out.put("_небезопасный_ключ",HIDDEN); continue; }
                out.put(key,SECRET_KEY.matcher(key).find()?HIDDEN:scrub(source.opt(key),depth+1));
            }
            return out;
        }
        if(value instanceof JSONArray) {
            JSONArray source=(JSONArray)value, out=new JSONArray(); int max=Math.min(30,source.length());
            for(int i=0;i<max;i++) out.put(scrub(source.opt(i),depth+1));
            if(source.length()>max) out.put("[другие элементы скрыты]"); return out;
        }
        if(value instanceof String) {
            String s=(String)value;
            if(s.length()<=10000 && (s.trim().startsWith("{") || s.trim().startsWith("["))) {
                try { return scrub(new JSONTokener(s).nextValue(),depth+1); } catch(Exception ignored) { }
            }
            return cleanText(s);
        }
        return value==null?JSONObject.NULL:value;
    }
    private static String cleanText(String value) {
        String s=BEARER.matcher(value).replaceAll("Bearer [скрыто]");
        s=JWT.matcher(s).replaceAll(HIDDEN); s=ASSIGNMENT.matcher(s).replaceAll("$1=[скрыто]");
        s=URL_QUERY.matcher(s).replaceAll("$1?[скрыто]"); s=EMAIL.matcher(s).replaceAll("[email скрыт]");
        // Long opaque strings may be tokens, while normal short game text remains readable.
        if(s.length()>64 && s.matches("[A-Za-z0-9_+/.=-]+")) return HIDDEN;
        return s.length()>1000 ? s.substring(0,1000)+"… [сокращено]" : s;
    }
    static String host(String host) {
        if(host==null || host.length()>253 || !host.matches("[a-zA-Z0-9](?:[a-zA-Z0-9.:-]{0,251}[a-zA-Z0-9])?")) return "[адрес скрыт]";
        return host;
    }
}
