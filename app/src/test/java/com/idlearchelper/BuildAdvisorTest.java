package com.idlearchelper;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class BuildAdvisorTest {
    private JSONObject sample() throws Exception { return new JSONObject("{\"characterClassName\":\"Druid\",\"activeCombatStance\":\"bulwark\",\"characterEquippedItems\":[{\"id\":7,\"name\":\"Gloves\",\"slot\":\"hands\",\"type\":\"gloves\",\"tier\":6,\"affix_positions\":[{\"affix\":{\"readable_bonus_name\":\"Pet Damage\",\"value\":12,\"range_min\":10,\"range_max\":20}},{\"affix\":null}]}],\"characterItemsPaginated\":{\"current_page\":1,\"last_page\":3,\"data\":[{\"id\":7,\"name\":\"Duplicate\"},{\"id\":8,\"name\":\"Helmet\",\"slot\":\"head\",\"tier\":5}]},\"newRunes\":[{\"name\":\"Rune A\",\"level\":3}]}"); }
    @Test public void knownItemsDedupeCoverageAndComparison() throws Exception {
        JSONObject d=sample(), config=new JSONObject("{\"itemization\":{\"tiers\":{\"6\":{\"max_level\":30,\"affix_slots\":5}}}}");
        assertEquals(2,BuildAdvisor.items(d).size());
        assertTrue(BuildAdvisor.coverage(d).contains("1 из 3"));
        String details=BuildAdvisor.itemDetails(BuildAdvisor.items(d).get(0),config);
        assertTrue(details.contains("20% диапазона")); assertTrue(details.contains("Урон питомца")); assertTrue(details.contains("уровень 30"));
        assertTrue(BuildAdvisor.compare(BuildAdvisor.items(d).get(0),BuildAdvisor.items(d).get(1),config).contains("Слоты различаются"));
        assertTrue(BuildAdvisor.opportunities(d).contains("20% диапазона"));
        assertTrue(BuildAdvisor.stance(d).contains("Оплот"));
        assertTrue(BuildAdvisor.runes(d).contains("Rune A"));
    }
    @Test public void missingFieldsNeverInventValues() throws Exception {
        JSONObject d=new JSONObject(); assertTrue(BuildAdvisor.items(d).isEmpty());
        assertTrue(BuildAdvisor.opportunities(d).contains("недоступны"));
        assertTrue(BuildAdvisor.runes(d).contains("не означает"));
        assertTrue(BuildAdvisor.coverage(d).contains("не загружен"));
        assertTrue(BuildAdvisor.stance(d).contains("нет данных"));
    }
    @Test public void invalidRangesDoNotCreateFakeAdvice() throws Exception {
        JSONObject d=new JSONObject("{\"characterEquippedItems\":[{\"name\":\"A\",\"affix_positions\":[{\"affix\":{\"value\":50,\"range_min\":10,\"range_max\":20}},{\"affix\":{\"value\":10,\"range_min\":10,\"range_max\":10}}]}]}");
        assertTrue(BuildAdvisor.opportunities(d).contains("недоступны"));
        assertFalse(BuildAdvisor.itemDetails(BuildAdvisor.items(d).get(0),null).contains("% диапазона"));
    }
}
