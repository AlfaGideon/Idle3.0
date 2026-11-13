package com.idlearchelper;

import org.junit.Test;
import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.Assert.*;

public class HumanFormatTest {
    @Test public void knownAndUnknownNamesRemainDistinguishable() {
        assertEquals("Максимальное здоровье", HumanFormat.label("maxHealth"));
        assertTrue(HumanFormat.known("maxHealth"));
        assertEquals("Без перевода: newStat", HumanFormat.label("newStat"));
        assertFalse(HumanFormat.known("newStat"));
        assertEquals("Бонусные характеристики / Без перевода: newStat / #1", HumanFormat.labelPath(new JSONArray().put("bonusStats").put("newStat").put(0)));
    }
    @Test public void valuesRetainTypesAndExactJson() {
        assertEquals("Да (true)", HumanFormat.humanValue(true));
        assertEquals("Нет (false)", HumanFormat.humanValue(false));
        assertEquals("Нет значения (null)", HumanFormat.humanValue(JSONObject.NULL));
        assertEquals("Пустая строка", HumanFormat.humanValue(""));
        assertEquals("\"a\\\"b\"", HumanFormat.jsonValue("a\"b"));
        assertEquals("1.25", HumanFormat.jsonValue(1.25));
        assertEquals("Число", HumanFormat.type(1.25));
        assertEquals("Строка", HumanFormat.type("1.25"));
    }
}
