package com.idlearchelper;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

/** Friendly labels are explicitly mapped; unknown fields retain their original names. */
final class HumanFormat {
    private static final Map<String,String> LABELS;
    private static final Map<String,String> WORDS;
    private static final Map<String,String> RARITIES;
    private static final Map<String,String> CLASSES;
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
        // Dashboard names observed in the user's companion client; unknown fields are kept intact.
        m.put("goldAmount","Количество золота"); m.put("rubyAmount","Количество рубинов"); m.put("arcstoneAmount","Количество аркстоунов"); m.put("arcstonesAmount","Количество аркстоунов");
        m.put("petShardsAmount","Осколки питомцев"); m.put("elementalShardsAmount","Осколки стихий"); m.put("petEggsAmount","Яйца питомцев");
        m.put("currentHealth","Текущее здоровье"); m.put("currentHealthPercentage","Текущее здоровье, %");
        m.put("monsterLevel","Уровень монстров"); m.put("monsterLevelProgress","Прогресс уровня монстров"); m.put("monsterLevelPaused","Пауза уровня монстров");
        m.put("selectedMonsterLevel","Выбранный уровень монстров"); m.put("currentMonster","Текущий монстр"); m.put("currentMonsterInstance","Текущий экземпляр монстра");
        m.put("currentBiome","Текущий биом"); m.put("unlockedBiomeIds","Открытые биомы"); m.put("isRespawning","Ожидание возрождения");
        m.put("health","Здоровье"); m.put("damage","Урон"); m.put("current","Текущее значение"); m.put("next","Следующее значение");
        m.put("percentage","Процент"); m.put("percent","Процент"); m.put("progress","Прогресс"); m.put("levelProgress","Прогресс уровня");
        m.put("kills","Убийства"); m.put("kills_required","Необходимое количество убийств"); m.put("required","Требуется");
        m.put("characterEquippedItems","Надетая экипировка"); m.put("characterItemsPaginated","Предметы персонажа по страницам");
        m.put("equippedTalismans","Надетые талисманы"); m.put("characterPets","Питомцы персонажа"); m.put("activeCombatStance","Активная боевая стойка");
        m.put("activeClassSkillPresetSlot","Активный набор классовых навыков"); m.put("activeGearPresetSlot","Активный набор экипировки");
        m.put("activePetPresetSlot","Активный набор питомцев"); m.put("activeTreePresetSlot","Активный набор дерева навыков");
        m.put("gearPresetSlots","Наборы экипировки"); m.put("classSkillPresetSlots","Наборы классовых навыков");
        m.put("newLoot","Новая добыча"); m.put("newGems","Новые самоцветы"); m.put("newRunes","Новые руны");
        m.put("bag","Сумка"); m.put("bag_cap","Вместимость сумки"); m.put("items","Предметы"); m.put("data","Данные");
        m.put("gemSockets","Гнёзда самоцветов"); m.put("sockets","Гнёзда"); m.put("gem","Самоцвет"); m.put("gemDust","Пыль самоцветов");
        m.put("pristine","Нетронутый"); m.put("secondaries","Вторичные параметры"); m.put("stat","Характеристика");
        m.put("rarity","Редкость"); m.put("rarity_slug","Код редкости"); m.put("quality","Качество"); m.put("family","Семейство");
        m.put("slug","Код"); m.put("key","Ключ"); m.put("label","Название"); m.put("count","Количество"); m.put("amount","Количество");
        m.put("index","Индекс"); m.put("total","Всего"); m.put("last_page","Последняя страница");
        m.put("is_equipped","Надет"); m.put("unlocked","Открыто"); m.put("locked","Закрыто"); m.put("is_locked","Заблокировано");
        m.put("active","Активно"); m.put("enabled","Включено"); m.put("completed","Завершено"); m.put("ready","Готово");
        m.put("can_evolve","Можно развить"); m.put("can_upgrade","Можно улучшить"); m.put("can_enter","Можно войти");
        m.put("evolve_cost_rubies","Стоимость развития в рубинах"); m.put("upgrade_cost","Стоимость улучшения"); m.put("next_cost","Стоимость следующего уровня");
        m.put("max_tier_level","Максимальный уровень качества"); m.put("star_tier","Звёздный уровень"); m.put("gild_level","Уровень позолоты");
        m.put("awaken_level","Уровень пробуждения"); m.put("awaken_boost_count","Число усилений пробуждения");
        m.put("affixes","Аффиксы"); m.put("affix","Аффикс"); m.put("affix_type","Тип аффикса"); m.put("affix_positions","Позиции аффиксов");
        m.put("implicit_bonus","Встроенный бонус"); m.put("secondary_implicit_bonus","Второй встроенный бонус");
        m.put("readable_bonus_name","Название бонуса"); m.put("bonus_type","Тип бонуса"); m.put("boost_count","Количество усилений");
        m.put("evolve_affixes_at_max","Развитие аффиксов до максимума"); m.put("evolve_affixes_required","Требуемые аффиксы развития");
        m.put("craftingMaterials","Материалы для создания"); m.put("weapon","Оружие"); m.put("equipped_sub_slot","Дополнительная ячейка экипировки");
        m.put("allowed_classes","Допустимые классы"); m.put("ranks","Уровни навыков"); m.put("point_count","Количество очков");
        m.put("treePoints","Очки дерева навыков"); m.put("availablePassivePoints","Доступные очки пассивных навыков");
        m.put("passives","Пассивные навыки"); m.put("skills","Навыки"); m.put("skill_id","ID навыка"); m.put("max_rank","Максимальный ранг");
        m.put("per_rank","За ранг"); m.put("class_skill_trees","Деревья классовых навыков"); m.put("class_skill_caps","Ограничения классовых навыков");
        m.put("cap","Ограничение"); m.put("base","Базовое значение"); m.put("contributions","Источники бонуса");
        m.put("guild","Гильдия"); m.put("top_players","Лучшие игроки"); m.put("character_name","Имя персонажа");
        m.put("guild_boss_can_fight","Можно сразиться с боссом гильдии"); m.put("isBossOnCooldown","Босс на перезарядке");
        m.put("bossOnCooldown","Босс на перезарядке"); m.put("bossCooldownExpirationTime","Окончание перезарядки босса");
        m.put("bossCooldownRemainingMs","Перезарядка босса, мс"); m.put("isDungeonOnCooldown","Подземелье на перезарядке");
        m.put("dungeonCooldownExpirationTime","Окончание перезарядки подземелья"); m.put("boss_can_fight","Можно сразиться с боссом");
        m.put("canFightBoss","Можно сразиться с боссом"); m.put("boss_ready","Босс доступен"); m.put("pendingPetHatch","Ожидается вылупление питомца");
        m.put("activeTimeBoosts","Активные ускорения"); m.put("unseen_achievements","Непросмотренные достижения");
        m.put("unread_mail_count","Непрочитанные письма"); m.put("pending_friend_requests_count","Ожидающие заявки в друзья");
        m.put("remaining_ms","Осталось миллисекунд"); m.put("remaining_seconds","Осталось секунд");
        m.put("autoConvertFragmentsEnabled","Автоконвертация фрагментов"); m.put("autoConvertEssencesEnabled","Автоконвертация эссенций");
        m.put("autoConvertMonsterMaterialsEnabled","Автоконвертация материалов монстров"); m.put("gearSetStatus","Состояние комплекта экипировки");
        m.put("equipped_count","Количество надетых предметов"); m.put("active_set","Активный комплект"); m.put("pieces","Предметы комплекта");
        m.put("attackDamageBreakdown","Источники урона атаки"); m.put("petDamageBreakdown","Источники урона питомца");
        m.put("petDamageFinalAvg","Средний итоговый урон питомца"); m.put("critOvercapBreakdown","Превышение предела критического удара");
        m.put("ratio","Соотношение"); m.put("overcap_percent","Превышение предела, %"); m.put("bonus_crit_damage","Бонус критического урона");
        m.put("drop_bonus_type","Тип бонуса выпадения"); m.put("drop_bonus_value","Величина бонуса выпадения");
        m.put("special_bonus_type","Тип особого бонуса"); m.put("special_bonus_active_value","Активное значение особого бонуса");
        m.put("range_min","Минимальное значение диапазона"); m.put("range_max","Максимальное значение диапазона"); m.put("roll_quality","Качество результата");
        m.put("setting_level","Уровень настройки"); m.put("compound_level","Составной уровень"); m.put("compound_effective_cap","Эффективное ограничение составного параметра");
        m.put("effectivePetMasteryLevel","Эффективный уровень мастерства питомца"); m.put("talismanPity","Гарантия талисмана");
        m.put("talismanDailyRemaining","Осталось сегодня для талисмана"); m.put("talismanInscriptionQuote","Стоимость надписи талисмана");
        m.put("inscription_totals","Итоги надписей"); m.put("ascent_points","Очки восхождения"); m.put("announcement","Объявление");
        LABELS=Collections.unmodifiableMap(m);
        Map<String,String> w=new HashMap<>();
        String[][] words={
            {"character","персонаж"},{"monster","монстр"},{"boss","босс"},{"guild","гильдия"},{"player","игрок"},
            {"pet","питомец"},{"item","предмет"},{"gear","экипировка"},{"weapon","оружие"},{"rune","руна"},
            {"gem","самоцвет"},{"talisman","талисман"},{"skill","навык"},{"class","класс"},{"stat","характеристика"},
            {"damage","урон"},{"health","здоровье"},{"defense","защита"},{"gold","золото"},{"ruby","рубин"},
            {"material","материал"},{"materials","материалы"},{"fragment","фрагмент"},{"fragments","фрагменты"},
            {"essence","эссенция"},{"essences","эссенции"},{"chance","шанс"},{"bonus","бонус"},{"value","значение"},
            {"level","уровень"},{"max","максимум"},{"min","минимум"},{"total","всего"},{"current","текущий"},
            {"remaining","осталось"},{"base","базовый"},{"cost","стоимость"},{"upgrade","улучшение"},{"drop","выпадение"},
            {"tier","качество"},{"multiplier","множитель"},{"percent","процент"},{"percentage","процент"},{"pct","процент"},
            {"count","количество"},{"amount","количество"},{"time","время"},{"seconds","секунды"},{"enabled","включено"},
            {"active","активный"},{"cooldown","перезарядка"},{"slot","ячейка"},{"slots","ячейки"},{"progress","прогресс"},
            {"rate","частота"},{"rank","ранг"},{"points","очки"},{"point","очко"},{"experience","опыт"},{"exp","опыт"},
            {"attack","атака"},{"critical","критический"},{"hit","удар"},{"speed","скорость"},{"per","за"},
            {"is","признак"},{"can","можно"},{"id","ID"},{"name","имя"},{"type","тип"},{"new","новый"},
            {"bonus","бонус"},{"enabled","включено"},{"requirement","требование"},{"requirements","требования"},
            {"tiered","по качеству"},{"growth","рост"},{"failure","неудача"},{"reduction","снижение"},{"duration","длительность"}
        };
        for(String[] pair:words) w.put(pair[0],pair[1]);
        WORDS=Collections.unmodifiableMap(w);
        Map<String,String> r=new HashMap<>(); r.put("normal","Обычный"); r.put("uncommon","Необычный"); r.put("rare","Редкий"); r.put("epic","Эпический"); r.put("legendary","Легендарный"); r.put("infernal","Инфернальный"); r.put("mythic","Мифический"); RARITIES=Collections.unmodifiableMap(r);
        Map<String,String> c=new HashMap<>(); c.put("warrior","Воин"); c.put("mage","Маг"); c.put("ranger","Следопыт"); c.put("druid","Друид"); c.put("rogue","Разбойник"); CLASSES=Collections.unmodifiableMap(c);
    }
    static boolean known(String key) { return LABELS.containsKey(key); }
    static String label(String key) {
        String translated=LABELS.get(key);
        if (translated != null) return translated;
        String readable=key.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_',' ').replace('-', ' ');
        StringBuilder translatedWords=new StringBuilder(); boolean unknown=false;
        for(String word:readable.split("\\s+")) {
            if(word.isEmpty()) continue;
            if(translatedWords.length()>0) translatedWords.append(' ');
            String part=WORDS.get(word.toLowerCase(Locale.ROOT));
            if(part!=null) translatedWords.append(part); else { translatedWords.append(word); unknown=true; }
        }
        if(translatedWords.length()==0) return key;
        translatedWords.setCharAt(0, Character.toUpperCase(translatedWords.charAt(0)));
        return translatedWords.toString() + (unknown ? " (частичный перевод)" : " (автоматический перевод)");
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
        if(value==null || value==JSONObject.NULL) return "Нет значения";
        if(value instanceof Boolean) return (Boolean)value ? "Да" : "Нет";
        if(value instanceof String && ((String)value).isEmpty()) return "Пустая строка";
        return String.valueOf(value);
    }
    static String humanValue(String key, Object value) {
        if(value instanceof String) {
            String lower=((String)value).toLowerCase(Locale.ROOT);
            String translated=null;
            if("rarity".equals(key) || "rarity_slug".equals(key)) translated=RARITIES.get(lower);
            else if("characterClassName".equals(key)) translated=CLASSES.get(lower);
            if(translated!=null) return translated;
        }
        return humanValue(value);
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
