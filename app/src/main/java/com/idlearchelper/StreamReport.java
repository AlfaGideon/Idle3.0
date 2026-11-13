package com.idlearchelper;

import org.json.JSONObject;
import java.util.*;

/** Strict native allowlists: no raw URL, response, header, cookie or event content reaches logs. */
final class StreamReport {
    private static final Set<String> SEGMENTS = new HashSet<>(Arrays.asList("api","app","ws","websocket","socket","socket.io","events","event","stream","sse","broadcasting","channels","chat","dashboard","realtime","live","subscribe","notifications","updates","connect","_"));
    private static final Set<String> SHAPES = new HashSet<>(Arrays.asList("binary","large-text","json-null","json-array","json-scalar","json-object","text"));
    private static final Set<String> KEYS = new HashSet<>(Arrays.asList("type","event","data","payload","action","channel","timestamp","state","status","message"));
    static String render(String raw) {
        if (raw == null || raw.length() > 1024) return null;
        try {
            JSONObject obj=new JSONObject(raw);
            String kind=obj.optString("kind"), stage=obj.optString("stage"), location=obj.optString("location"), shape=obj.optString("shape");
            boolean probe=kind.equals("probe") && stage.equals("ready");
            boolean socket=(kind.equals("ws") || kind.equals("sse")) && Arrays.asList("created","open","error","close","message").contains(stage);
            boolean stream=(kind.equals("fetch") || kind.equals("xhr")) && stage.equals("stream");
            if (!probe && !socket && !stream) return null;
            String[] parts=location.split("/",-1);
            if(parts.length>9 || !(parts[0].equals("idlearc.com") || parts[0].equals("www.idlearc.com") || parts[0].equals("external") || parts[0].equals("unknown"))) return null;
            for(int i=1;i<parts.length;i++) if(!parts[i].isEmpty() && !SEGMENTS.contains(parts[i])) return null;
            if(stage.equals("message")) {
                if(!safeShape(shape)) return null;
            } else if(!shape.isEmpty()) return null;
            String display=location.replace("external", "внешний сервер").replace("unknown", "адрес не определён").replace("_", "…");
            if(probe) return "Сканирование началось: " + display;
            String label=kind.equals("ws") ? "WebSocket" : kind.equals("sse") ? "EventSource (SSE)" : kind.equals("fetch") ? "SSE через fetch" : "SSE через XHR";
            String description=stage.equals("created")?"создано":stage.equals("open")?"подключено":stage.equals("error")?"ошибка":stage.equals("close")?"закрыто":stage.equals("stream")?"поток обнаружен":"сообщение";
            return label + ": " + description + " — " + display + (shape.isEmpty()?"":"; формат: " + shape);
        } catch(Exception ignored) { return null; }
    }
    private static boolean safeShape(String shape) {
        if(SHAPES.contains(shape)) return true;
        if(!shape.startsWith("json-object:")) return false;
        String[] keys=shape.substring("json-object:".length()).split(",",-1);
        if(keys.length==0 || keys.length>10) return false;
        for(String key:keys) if(!KEYS.contains(key)) return false;
        return true;
    }
}
