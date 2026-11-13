package com.idlearchelper;

import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Strict bridge schema. Values are displayed exactly; no unrelated page data enters the UI. */
final class AscentEvent {
    final String kind, pool, tier, overflow, contributors, lifetime;
    private AscentEvent(String kind,String pool,String tier,String overflow,String contributors,String lifetime) {
        this.kind=kind; this.pool=pool; this.tier=tier; this.overflow=overflow; this.contributors=contributors; this.lifetime=lifetime;
    }
    static AscentEvent parse(String raw) {
        if(raw==null || raw.length()>1024) return null;
        try {
            JSONObject obj=new JSONObject(raw); String kind=obj.optString("kind");
            Set<String> states=new HashSet<>(Arrays.asList("connecting","open","subscribed","closed","error"));
            if(states.contains(kind)) return new AscentEvent(kind,null,null,null,null,null);
            if(!kind.equals("ascent")) return null;
            String pool=number(obj,"pool"), tier=number(obj,"tier");
            if(pool==null || tier==null || tier.contains(".")) return null;
            return new AscentEvent(kind,pool,tier,number(obj,"overflow"),number(obj,"contributors"),number(obj,"lifetime"));
        } catch(Exception e) { return null; }
    }
    private static String number(JSONObject obj,String key) {
        Object value=obj.opt(key); if(!(value instanceof String)) return null;
        String s=(String)value; return s.length()<=64 && s.matches("[0-9]+(?:\\.[0-9]+)?") ? s : null;
    }
}
