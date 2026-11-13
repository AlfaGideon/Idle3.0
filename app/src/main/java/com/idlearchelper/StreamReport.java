package com.idlearchelper;

import org.json.JSONObject;
import java.util.*;

/** Strict native allowlists: no raw URL, response, header, cookie or event content reaches logs. */
final class StreamReport {
    private static final Set<String> SEGMENTS = new HashSet<>(Arrays.asList("api","app","ws","websocket","socket","socket.io","events","event","stream","sse","broadcasting","channels","chat","dashboard","realtime","live","subscribe","notifications","updates","connect","_"));
    private static final Set<String> SHAPES = new HashSet<>(Arrays.asList("binary","large-text","json-null","json-array","json-scalar","json-object","text"));
    private static final Set<String> KEYS = new HashSet<>(Arrays.asList("type","event","data","payload","action","channel","timestamp","state","status","message"));
    private static final Set<String> DATA_KEYS = new HashSet<>(Arrays.asList("character","characterId","character_id","level","experience","exp","gold","goldAmount","ruby","rubies","rubyAmount","arcstone","arcstones","arcstoneAmount","currentHealth","maxHealth","health","damage","attackDamage","monsterLevel","monster","inventory","items","equipment","pets","skills","progress","dashboard","balance","currency","amount","player","user","text","message","sender","channel","type","event","updated","status","data","payload"));
    static String render(String raw) {
        if (raw == null || raw.length() > 1024) return null;
        try {
            JSONObject obj=new JSONObject(raw);
            String kind=obj.optString("kind"), stage=obj.optString("stage"), location=obj.optString("location"), shape=obj.optString("shape"), meta=obj.optString("meta");
            boolean probe=kind.equals("probe") && stage.equals("ready");
            boolean socket=(kind.equals("ws") || kind.equals("sse")) && Arrays.asList("created","open","error","close","message","sent").contains(stage);
            boolean stream=(kind.equals("fetch") || kind.equals("xhr")) && stage.equals("stream");
            if (!probe && !socket && !stream) return null;
            String[] parts=location.split("/",-1);
            if(parts.length>9 || !(parts[0].equals("idlearc.com") || parts[0].equals("www.idlearc.com") || parts[0].equals("external") || parts[0].equals("unknown") || parts[0].matches("ws-[a-z]{2}[0-9]{0,2}\\.pusher\\.com"))) return null;
            for(int i=1;i<parts.length;i++) if(!parts[i].isEmpty() && !SEGMENTS.contains(parts[i])) return null;
            if(stage.equals("message") || stage.equals("sent")) {
                if(!safeShape(shape) || !safeMetadata(meta)) return null;
            } else if(!shape.isEmpty() || !meta.isEmpty()) return null;
            String display=location.replace("external", "внешний сервер").replace("unknown", "адрес не определён").replace("_", "…");
            if(probe) return "Сканирование началось: " + display;
            String label=kind.equals("ws") ? "WebSocket" : kind.equals("sse") ? "EventSource (SSE)" : kind.equals("fetch") ? "SSE через fetch" : "SSE через XHR";
            String description=stage.equals("created")?"создано":stage.equals("open")?"подключено":stage.equals("error")?"ошибка":stage.equals("close")?"закрыто":stage.equals("stream")?"поток обнаружен":stage.equals("sent")?"отправлено":"сообщение";
            return label + ": " + description + " — " + display + (shape.isEmpty()?"":"; формат: " + shape) + describeMetadata(meta);
        } catch(Exception ignored) { return null; }
    }
    private static boolean safeMetadata(String meta) {
        if(meta.isEmpty()) return true;
        String[] p=meta.split("\\|",-1);
        if(p.length!=4 || !Arrays.asList("","connected","subscribed","subscribe","unsubscribe","ping","pong","error","subscription-error","custom").contains(p[0])
                || !Arrays.asList("","private","presence","public").contains(p[1])
                || !Arrays.asList("","chat","guild","dashboard","character","combat","boss","other").contains(p[2])) return false;
        if(p[3].isEmpty()) return true;
        String[] fields=p[3].split(",",-1);
        if(fields.length>16) return false;
        for(String field:fields) if(!DATA_KEYS.contains(field)) return false;
        return true;
    }
    private static String describeMetadata(String meta) {
        if(meta.isEmpty()) return "";
        String[] p=meta.split("\\|",-1);
        StringBuilder b=new StringBuilder();
        if(!p[0].isEmpty()) b.append("; событие: ").append(p[0]);
        if(!p[1].isEmpty()) b.append("; канал: ").append(p[1]);
        if(!p[2].isEmpty()) b.append("; предполагаемая категория: ").append(p[2]);
        if(!p[3].isEmpty()) b.append("; поля данных: ").append(p[3]);
        return b.toString();
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
