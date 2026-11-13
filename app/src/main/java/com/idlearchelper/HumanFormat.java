package com.idlearchelper;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Friendly labels are explicitly mapped; unknown fields retain their original names. */
final class HumanFormat {
    private static final Map<String,String> LABELS;
    static {
        Map<String,String> m=new HashMap<>();
        m.put("level","Уровень"); m.put("name","Название"); m.put("id","Идентификатор"); m.put("type","Тип");
        m.put("value","Значение"); m.put("min","Минимум"); m.put("max","Максимум"); m.put("version","Версия");
        m.put("gold","Золото"); m.put("rubies","Рубины"); m.put("ruby","Рубин"); m.put("experience","Опыт");
        m.put("characterClassId","ID класса персонажа"); m.put("characterClassName","Класс персонажа");
        m.put("gear_score","Рейтинг экипировки"); m.put("maxHealth","Максимальное здоровье"); m.put("defense","Защита");
        m.put("attackDamage","Урон атаки"); m.put("damageReduction","Снижение урона");
        m.put("damageReductionUncapped","Снижение урона до ограничения"); m.put("criticalStrikeChance","Шанс критического удара");
        m.put("criticalDamage","Критический урон"); m.put("doubleHitChance","Шанс двойного удара");
        m.put("doubleDamageChance","Шанс двойного урона"); m.put("blockChance","Шанс блока");
        m.put("bossDamage","Урон по боссам"); m.put("extraKillChance","Шанс дополнительного убийства");
        m.put("retaliation","Ответный удар"); m.put("expectedPerExchange","Ожидаемое значение за обмен ударами");
        m.put("bonusStats","Бонусные характеристики"); m.put("primaryStats","Основные характеристики");
        m.put("strength","Сила"); m.put("dexterity","Ловкость"); m.put("intelligence","Интеллект");
        m.put("petDamagePercent","Бонус урона питомца"); m.put("petDamageTrueMultiplier","Множитель урона питомца");
        m.put("expGainPercent","Бонус получения опыта"); m.put("goldGainPercent","Бонус получения золота");
        m.put("lifeOnHit","Здоровье за удар"); m.put("lifeOnKill","Здоровье за убийство");
        m.put("bloodthirstPercent","Кровожадность"); m.put("totalItemDropChance","Шанс выпадения предметов");
        m.put("effectiveItemDropMultiplier","Множитель выпадения предметов"); m.put("totalMaterialDropChance","Шанс выпадения материалов");
        m.put("effectiveMaterialDropMultiplier","Множитель выпадения материалов"); m.put("totalRuneDropChance","Шанс выпадения рун");
        m.put("effectiveRuneDropMultiplier","Множитель выпадения рун"); m.put("totalRubyDropChance","Шанс выпадения рубинов");
        m.put("effectiveRubyDropMultiplier","Множитель выпадения рубинов"); m.put("dodge_chance","Шанс уклонения");
        m.put("dodge_chance_uncapped","Шанс уклонения до ограничения"); m.put("petSpecialBonuses","Особые бонусы питомца");
        m.put("ascentBonuses","Бонусы восхождения"); m.put("all_skills","Все навыки"); m.put("pet_egg_drop_pct","Шанс выпадения яиц питомца");
        m.put("itemization","Параметры предметов"); m.put("tiers","Редкости / уровни качества");
        m.put("max_level","Максимальный уровень"); m.put("affix_slots","Ячейки аффиксов"); m.put("multiplier","Множитель");
        m.put("increment","Прирост"); m.put("base_bonus","Базовый бонус"); m.put("level_upgrade_costs","Стоимость повышения уровня");
        m.put("tier_upgrade_costs","Стоимость повышения качества"); m.put("level_upgrade_materials","Материалы повышения уровня");
        m.put("tier_upgrade_materials","Материалы повышения качества"); m.put("fragment_cost_multipliers","Множители стоимости фрагментов");
        m.put("level_cost_growth_rate","Темп роста стоимости уровня"); m.put("upgrade_failure","Неудача улучшения");
        m.put("tier_upgrade_failure","Неудача повышения качества"); m.put("tier_base","Базовое значение по качеству");
        m.put("tier_threshold","Порог по качеству"); m.put("per_level_increase","Прирост за уровень");
        m.put("max_failure_chance","Максимальный шанс неудачи"); m.put("fail_refund_pct","Возврат при неудаче");
        m.put("monster_material_requirements","Требования к материалам монстров"); m.put("quantity","Количество");
        m.put("slot","Ячейка"); m.put("tier","Качество"); m.put("tier_level","Уровень качества");
        LABELS=Collections.unmodifiableMap(m);
    }
    static boolean known(String key) { return LABELS.containsKey(key); }
    static String label(String key) {
        String translated=LABELS.get(key);
        return translated == null ? "Без перевода: " + key : translated;
    }
    static String labelPath(JSONArray path) {
        StringBuilder s=new StringBuilder();
        for(int i=0;i<path.length();i++) {
            if(i>0) s.append(" / "); Object part=path.opt(i);
            s.append(part instanceof String ? label((String)part) : "#"+(((Number)part).intValue()+1));
        }
        return s.toString();
    }
    static String humanValue(Object value) {
        if(value==null || value==JSONObject.NULL) return "Нет значения (null)";
        if(value instanceof Boolean) return (Boolean)value ? "Да (true)" : "Нет (false)";
        if(value instanceof String && ((String)value).isEmpty()) return "Пустая строка";
        return String.valueOf(value);
    }
    static String jsonValue(Object value) {
        if(value==null || value==JSONObject.NULL) return "null";
        if(value instanceof String) return JSONObject.quote((String)value);
        return String.valueOf(value);
    }
    static String type(Object value) {
        if(value==null || value==JSONObject.NULL) return "null";
        if(value instanceof Boolean) return "Логическое";
        if(value instanceof Number) return "Число";
        if(value instanceof String) return "Строка";
        if(value instanceof JSONArray) return "Массив";
        if(value instanceof JSONObject) return "Объект";
        return "Неизвестный тип";
    }
}
