package com.idlearchelper;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Read-only analysis of the explicitly observed dashboard fields. */
final class BuildAdvisor {
    static final class Item {
        final JSONObject json; final String source;
        Item(JSONObject json,String source) { this.json=json; this.source=source; }
        String name() { return field(json,"name",field(json,"type","Предмет")); }
        String slot() { return field(json,"slot",""); }
        String title() { return name()+" · "+(slot().isEmpty()?"слот не указан":slot())+" · "+source; }
    }
    static String field(JSONObject obj,String key,String fallback) {
        if(obj==null) return fallback;
        Object v=obj.opt(key);
        return v instanceof String || v instanceof Number ? String.valueOf(v) : fallback;
    }
    static String val(JSONObject obj,String key) { return field(obj,key,"нет данных"); }
    static String affixName(JSONObject aff) {
        String name=field(aff,"readable_bonus_name",field(aff,"name",field(aff,"type","Аффикс")));
        switch(name.toLowerCase(Locale.ROOT)) {
            case "intelligence": return "Интеллект";
            case "dexterity": return "Ловкость";
            case "strength": return "Сила";
            case "critical strike chance": return "Шанс критического удара";
            case "critical damage": return "Критический урон";
            case "pet damage": return "Урон питомца";
            case "double hit chance": return "Шанс двойного удара";
            case "double damage chance": return "Шанс двойного урона";
            case "attack damage": return "Урон атаки";
            case "defense": return "Защита";
            default: return name;
        }
    }
    static String stance(JSONObject d) {
        String v=val(d,"activeCombatStance");
        switch(v.toLowerCase(Locale.ROOT)) { case "bulwark": return "Оплот (bulwark)"; case "harmony": return "Гармония (harmony)"; case "fierce": return "Агрессия (fierce)"; default:return v; }
    }
    static String numeric(JSONObject obj,String key) {
        if(obj==null) return null;
        Object value=obj.opt(key); if(!(value instanceof Number) && !(value instanceof String)) return null;
        try { double d=Double.parseDouble(String.valueOf(value)); return Double.isNaN(d)||Double.isInfinite(d)?null:String.valueOf(value); } catch(Exception e) { return null; }
    }
    static List<Item> items(JSONObject dashboard) {
        List<Item> result=new ArrayList<>(); Set<String> ids=new HashSet<>();
        if(dashboard==null) return result;
        add(result,ids,dashboard.optJSONArray("characterEquippedItems"),"надето");
        JSONObject paged=dashboard.optJSONObject("characterItemsPaginated");
        if(paged!=null) add(result,ids,paged.optJSONArray("data"),"инвентарь · страница API");
        add(result,ids,dashboard.optJSONArray("newLoot"),"новая добыча");
        return result;
    }
    private static void add(List<Item> result,Set<String> ids,JSONArray items,String source) {
        if(items==null) return;
        for(int i=0;i<items.length() && result.size()<300;i++) {
            JSONObject obj=items.optJSONObject(i); if(obj==null) continue;
            String id=field(obj,"id",""); if(!id.isEmpty() && !ids.add(id)) continue;
            result.add(new Item(obj,source));
        }
    }
    static String coverage(JSONObject dashboard) {
        if(dashboard==null) return "Нет данных персонажа.";
        List<String> missing=new ArrayList<>();
        if(dashboard.optJSONArray("characterEquippedItems")==null) missing.add("экипировка");
        JSONObject paged=dashboard.optJSONObject("characterItemsPaginated");
        String inventory;
        if(paged==null || paged.optJSONArray("data")==null) inventory="Инвентарь целиком не загружен; доступна только новая добыча, если она есть.";
        else {
            int page=paged.optInt("current_page",1), last=paged.optInt("last_page",-1);
            inventory=last>1?"Инвентарь: страница "+page+" из "+last+"; остальные страницы не учтены.":last==1?"Инвентарь: одна страница API.":"Инвентарь: доступная страница API; полнота неизвестна.";
        }
        return (missing.isEmpty()?"":"Нет данных: "+String.join(", ",missing)+". ")+inventory;
    }
    static String goal(int mode) {
        if(mode==1) return "Боссы: сопоставляйте урон по боссам, урон атаки и питомцев с выживаемостью. Проверяйте результат на одном и том же боссе.";
        if(mode==2) return "Фарм: сравнивайте скорость убийств и бонусы выпадения предметов, рун и материалов. Больше дропа бесполезно, если скорость падает слишком сильно.";
        return "Продвижение: сопоставляйте урон и выживаемость. Проверяйте обе сборки на одном уровне монстров и без смены других условий.";
    }
    static String character(JSONObject d) {
        JSONObject bonus=d.optJSONObject("bonusStats");
        StringBuilder b=new StringBuilder();
        line(b,"Класс",HumanFormat.humanValue("characterClassName",d.opt("characterClassName")));
        line(b,"Уровень",val(d,"level")); line(b,"Рейтинг экипировки",val(d,"gear_score"));
        line(b,"Стойка",stance(d));
        for(String key:new String[]{"maxHealth","defense","damageReduction","criticalStrikeChance","criticalDamage","doubleHitChance","doubleDamageChance","bossDamage"}) if(d.has(key)) line(b,HumanFormat.label(key),HumanFormat.humanValue(d.opt(key)));
        if(bonus!=null) for(String key:new String[]{"petDamagePercent","petDamageFinalAvg","totalItemDropChance","totalRuneDropChance","totalMaterialDropChance"}) if(bonus.has(key)) line(b,HumanFormat.label(key),HumanFormat.humanValue(bonus.opt(key)));
        return b.toString();
    }
    static void line(StringBuilder b,String name,String value) { b.append(name).append(": ").append(value).append('\n'); }
    static String itemDetails(Item item, JSONObject config) {
        JSONObject j=item.json; StringBuilder b=new StringBuilder();
        line(b,"Источник",item.source); line(b,"Слот",val(j,"slot")); line(b,"Тип",val(j,"type"));
        for(String key:new String[]{"level","tier","tier_level","awaken_level","gild_level"}) if(j.has(key)) line(b,HumanFormat.label(key),val(j,key));
        if(j.has("drop_bonus_type")) line(b,"Бонус предмета",val(j,"drop_bonus_type")+" · "+val(j,"drop_bonus_value"));
        for(String key:new String[]{"implicit_bonus","secondary_implicit_bonus"}) { JSONObject x=j.optJSONObject(key); if(x!=null) line(b,HumanFormat.label(key),field(x,"type","бонус")+" · "+val(x,"value")); }
        JSONObject itemization=config==null?null:config.optJSONObject("itemization"); JSONObject tiers=itemization==null?null:itemization.optJSONObject("tiers");
        JSONObject tier=tiers==null?null:tiers.optJSONObject(field(j,"tier",""));
        if(tier!=null) line(b,"Предел качества по конфигурации","уровень "+val(tier,"max_level")+", ячеек аффиксов "+val(tier,"affix_slots"));
        b.append("\nАФФИКСЫ\n"); JSONArray arr=j.optJSONArray("affix_positions");
        if(arr==null) b.append("API не передал позиции аффиксов.\n");
        else if(arr.length()==0) b.append("Список позиций пуст.\n");
        else for(int i=0;i<Math.min(arr.length(),20);i++) {
            JSONObject pos=arr.optJSONObject(i); if(pos==null) continue; JSONObject aff=pos.optJSONObject("affix");
            b.append("• ").append(i+1).append(". ");
            if(aff==null) { b.append("нет данных об аффиксе\n"); continue; }
            b.append(affixName(aff)).append(": ").append(val(aff,"value"));
            String min=numeric(aff,"range_min"), max=numeric(aff,"range_max"), value=numeric(aff,"value");
            if(min!=null && max!=null && value!=null) {
                double lo=Double.parseDouble(min), hi=Double.parseDouble(max), n=Double.parseDouble(value);
                b.append(" (диапазон ").append(min).append("–").append(max).append(")");
                if(hi>lo && n>=lo && n<=hi) b.append(String.format(Locale.ROOT," · %.0f%% диапазона",100*(n-lo)/(hi-lo)));
            }
            if(aff.optBoolean("is_locked",false)) b.append(" · закреплён");
            b.append('\n');
        }
        b.append("\nПоложение в диапазоне — не оценка полезности аффикса. Проверяйте эффект и стоимость до перековки.");
        return b.toString();
    }
    static String compare(Item a,Item b,JSONObject config) {
        StringBuilder out=new StringBuilder();
        if(a.slot().isEmpty() || b.slot().isEmpty()) out.append("⚠ Слот одного из предметов неизвестен — проверьте совместимость.\n\n");
        else if(!a.slot().equalsIgnoreCase(b.slot())) out.append("⚠ Слоты различаются — предметы могут не заменять друг друга.\n\n");
        else out.append("Одинаковый слот: ").append(a.slot()).append(". Класс и дополнительные ограничения проверьте в игре.\n\n");
        out.append("А · ").append(a.name()).append("\n").append(itemDetails(a,config));
        out.append("\n\nБ · ").append(b.name()).append("\n").append(itemDetails(b,config));
        out.append("\n\nИТОГ: сравните бонусы под выбранную цель; качество и уровень сами по себе не доказывают, что предмет лучше. Данные могут быть неполными.");
        return out.toString();
    }
    static String opportunities(JSONObject d) {
        StringBuilder b=new StringBuilder(); int found=0, readable=0;
        for(Item item:items(d)) {
            if(!item.source.equals("надето")) continue;
            JSONArray positions=item.json.optJSONArray("affix_positions"); if(positions==null) continue;
            for(int i=0;i<positions.length();i++) {
                JSONObject pos=positions.optJSONObject(i); JSONObject aff=pos==null?null:pos.optJSONObject("affix"); if(aff==null) continue;
                String value=numeric(aff,"value"), min=numeric(aff,"range_min"), max=numeric(aff,"range_max");
                if(value==null || min==null || max==null) continue;
                double n=Double.parseDouble(value), lo=Double.parseDouble(min), hi=Double.parseDouble(max);
                if(hi<=lo || n<lo || n>hi) continue;
                readable++; double pct=100*(n-lo)/(hi-lo);
                if(pct<=30 && found<8) {
                    found++; b.append("• ").append(item.name()).append(" / ").append(affixName(aff))
                        .append(String.format(Locale.ROOT,": %.0f%% диапазона (%s из %s–%s)",pct,value,min,max)).append("\n");
                }
            }
        }
        if(readable==0) return "Диапазоны аффиксов надетых предметов не переданы или недоступны. Оценить качество бросков нельзя.";
        if(found==0) return "Среди "+readable+" проверенных аффиксов нет значений в нижних 30% диапазона. Это не оценка полезности сборки.";
        return "Проверьте эти значения в нижних 30% перед перековкой:\n"+b+"\nНизкий бросок не означает плохой тип. Перековка может стоить ресурсов и изменить аффикс; проверьте правила в игре.";
    }
    static String runes(JSONObject d) {
        JSONArray runes=d.optJSONArray("newRunes");
        if(runes==null) return "API не передал список newRunes. Это не означает, что у вас нет рун.";
        if(runes.length()==0) return "Новых рун в ответе нет. Это не полный список всех рун.";
        StringBuilder b=new StringBuilder("Новые руны из ответа API (не полный инвентарь):\n");
        for(int i=0;i<Math.min(60,runes.length());i++) {
            JSONObject r=runes.optJSONObject(i); if(r==null) continue;
            b.append("\n• ").append(field(r,"name",field(r,"type","Руна")));
            for(String k:new String[]{"rarity","level","tier","effect","description"}) if(r.has(k) && !(r.opt(k) instanceof JSONObject) && !(r.opt(k) instanceof JSONArray)) b.append("\n  ").append(HumanFormat.label(k)).append(": ").append(val(r,k));
            b.append('\n');
        }
        b.append("\nДля выбора сравните фактический эффект, условия срабатывания и совместимость в игре. Рейтинг рун без этих данных не рассчитывается."); return b.toString();
    }
}
