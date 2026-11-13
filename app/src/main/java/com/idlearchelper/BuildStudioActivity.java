package com.idlearchelper;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Interactive read-only build workspace; requests only when opened or manually refreshed. */
public final class BuildStudioActivity extends Activity {
    private static final int BG=0xff0b101c, PANEL=0xff19243a, TEXT=0xfff0f4fb, MUTED=0xffa7b7cb, GOLD=0xffffcc80, TEAL=0xff72d9d3;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private LinearLayout content, tabs; private TextView status; private JSONObject dashboard,config;
    private String configStatus="", fetched=""; private int mode, tab, sequence; private boolean destroyed;
    private List<BuildAdvisor.Item> items=new ArrayList<>();
    private int dp(float n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    private GradientDrawable bg(int color,int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private TextView text(String value,int size,int color) { TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setLineSpacing(dp(2),1); return t; }
    private Button button(String label,Runnable action) { Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(TEXT); b.setBackground(bg(0xff293a55,10)); b.setOnClickListener(v->action.run()); return b; }
    private void addSpace(LinearLayout parent,int height) { View v=new View(this); parent.addView(v,new LinearLayout.LayoutParams(1,dp(height))); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(12),dp(16),dp(8)); root.setBackgroundColor(BG); setContentView(root);
        root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(dp(16),dp(12)+i.getSystemWindowInsetTop(),dp(16),dp(8)+i.getSystemWindowInsetBottom());return i;});
        TextView tag=text("IDLE ARC  /  BUILD LAB",11,TEAL); tag.setTypeface(null,Typeface.BOLD); root.addView(tag);
        TextView title=text("Мастерская сборки",26,TEXT); title.setTypeface(null,Typeface.BOLD); root.addView(title);
        status=text("Загружаем данные…",12,MUTED); root.addView(status); addSpace(root,8);
        HorizontalScrollView nav=new HorizontalScrollView(this); nav.setHorizontalScrollBarEnabled(false); root.addView(nav);
        tabs=new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL); nav.addView(tabs);
        addSpace(root,8);
        ScrollView scroll=new ScrollView(this); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); scroll.addView(content);
        LinearLayout actions=new LinearLayout(this); root.addView(actions); Button refresh=button("Обновить",this::load); Button back=button("На главную",this::finish);
        LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,dp(46),1); a.rightMargin=dp(8); actions.addView(refresh,a); actions.addView(back,new LinearLayout.LayoutParams(0,dp(46),1));
        load();
    }
    private void load() {
        final int id=++sequence; dashboard=null; config=null; items.clear(); status.setText("Загружаем персонажа и конфигурацию…"); render();
        executor.execute(()-> {
            JSONObject d=null,c=null; String error=null, warning="";
            try { Object result=Api.get("/api/dashboard"); if(!(result instanceof JSONObject)) throw new Exception("Неизвестный формат данных персонажа."); d=(JSONObject)result; }
            catch(Exception e) { error=e.getMessage()==null?"Ошибка загрузки персонажа":e.getMessage(); }
            if(error==null) try { Object result=Api.get("/api/config/game"); if(result instanceof JSONObject) c=(JSONObject)result; else warning="Конфигурация недоступна."; }
            catch(Exception e) { warning="Конфигурация недоступна; пределы качества не показаны."; }
            final JSONObject dd=d, cc=c; final String err=error, warn=warning;
            runOnUiThread(()-> { if(destroyed || id!=sequence) return; dashboard=dd; config=cc; configStatus=warn;
                if(err!=null) { status.setText("Не удалось загрузить: "+err); content.removeAllViews(); card("НУЖНЫ ДАННЫЕ", "Проверьте соединение и вход в IdleArc, затем нажмите «Обновить».",TEAL); return; }
                items=BuildAdvisor.items(dashboard); fetched=DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()); status.setText("Снимок API · "+fetched+" · обновление вручную"); render();
            });
        });
    }
    private void render() {
        if(tabs==null || content==null) return; tabs.removeAllViews(); content.removeAllViews();
        String[] names={"Обзор","Экипировка","Сравнение","Руны и стойка"};
        for(int i=0;i<names.length;i++) { final int index=i; Button b=button(names[i],()->{tab=index;render();}); b.setTextColor(i==tab?BG:TEXT); b.setBackground(bg(i==tab?TEAL:PANEL,10)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(42)); p.rightMargin=dp(6); tabs.addView(b,p); }
        if(dashboard==null) { card("BUILD LAB", "Здесь появится разбор вашей сборки после загрузки данных.",TEAL); return; }
        if(tab==0) overview(); else if(tab==1) gear(); else if(tab==2) compare(); else runes();
    }
    private void card(String title,String body,int accent) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(15),dp(13),dp(15),dp(13)); box.setBackground(bg(PANEL,16));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(10); content.addView(box,p);
        TextView h=text(title,14,accent); h.setTypeface(null,Typeface.BOLD); box.addView(h); addSpace(box,6); box.addView(text(body,14,TEXT));
    }
    private void overview() {
        card("ВАША ЦЕЛЬ", "Выберите цель — подсказки изменятся. Это план для проверки в игре, а не гарантированная формула топа.",TEAL);
        LinearLayout goals=new LinearLayout(this); content.addView(goals); String[] names={"Прогресс","Боссы","Фарм"};
        for(int i=0;i<3;i++) {final int selected=i; Button b=button(names[i],()->{mode=selected;render();}); b.setTextColor(mode==i?BG:TEXT); b.setBackground(bg(mode==i?GOLD:0xff293a55,10)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(46),1); if(i<2)p.rightMargin=dp(4); goals.addView(b,p); } addSpace(content,10);
        card("ПЛАН",BuildAdvisor.goal(mode),GOLD);
        card("ПРОВЕРИТЬ АФФИКСЫ",BuildAdvisor.opportunities(dashboard),TEAL);
        card("СНИМОК ПЕРСОНАЖА",BuildAdvisor.character(dashboard),GOLD);
        card("ГРАНИЦЫ АНАЛИЗА",BuildAdvisor.coverage(dashboard)+(configStatus.isEmpty()?"":"\n"+configStatus)+"\nAPI не раскрывает полную модель боя: оптимальность и попадание в топ не гарантируются. Ничего в игре не меняется.",MUTED);
    }
    private void gear() {
        card("ЭКИПИРОВКА", "Нажмите на предмет, чтобы увидеть бонусы, диапазоны и данные качества. "+BuildAdvisor.coverage(dashboard),TEAL);
        if(items.isEmpty()) { card("НЕТ ПРЕДМЕТОВ", "В ответе API нет распознанных предметов. Обновите данные или проверьте «Данные персонажа».",MUTED); return; }
        for(BuildAdvisor.Item it:items) { Button b=button(it.title(),()->new android.app.AlertDialog.Builder(this).setTitle(it.name()).setMessage(BuildAdvisor.itemDetails(it,config)).setPositiveButton("Закрыть",null).show()); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.bottomMargin=dp(6);content.addView(b,p); }
    }
    private void compare() {
        card("СРАВНИТЬ ДВА ПРЕДМЕТА", "Выберите надетый предмет и альтернативу. Бонусы показаны рядом; автоматического выбора победителя нет.",TEAL);
        if(items.size()<2) { card("НЕДОСТАТОЧНО ДАННЫХ", "API передал меньше двух доступных предметов. "+BuildAdvisor.coverage(dashboard),MUTED); return; }
        List<String> labels=new ArrayList<>(); for(BuildAdvisor.Item i:items) labels.add(i.title());
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels);
        Spinner a=new Spinner(this), b=new Spinner(this); a.setAdapter(adapter); b.setAdapter(adapter); b.setSelection(1);
        content.addView(text("ПРЕДМЕТ А",12,GOLD)); content.addView(a,new LinearLayout.LayoutParams(-1,dp(50))); content.addView(text("ПРЕДМЕТ Б",12,GOLD)); content.addView(b,new LinearLayout.LayoutParams(-1,dp(50)));
        Button run=button("Сравнить параметры",()->{int x=a.getSelectedItemPosition(),y=b.getSelectedItemPosition(); if(x<0 || y<0 || x>=items.size() || y>=items.size()) return; new android.app.AlertDialog.Builder(this).setTitle("Сравнение предметов").setMessage(BuildAdvisor.compare(items.get(x),items.get(y),config)).setPositiveButton("Закрыть",null).show();});
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(50));p.topMargin=dp(8);content.addView(run,p);
        addSpace(content,10);card("ВАЖНО",BuildAdvisor.coverage(dashboard)+"\nПеред заменой проверьте класс, комплект, ограничения слота и итоговые характеристики в игре.",MUTED);
    }
    private void runes() {
        card("СТОЙКА", "Текущая стойка: "+BuildAdvisor.stance(dashboard)+"\nСравнивайте стойки на одинаковой цели: урон, время боя и выживаемость. Модель их влияния API не подтверждена, поэтому лучшая стойка не назначается автоматически.",TEAL);
        card("РУНЫ",BuildAdvisor.runes(dashboard),GOLD);
        card("НАБОРЫ", "Экипировка: "+BuildAdvisor.val(dashboard,"activeGearPresetSlot")+"\nНавыки: "+BuildAdvisor.val(dashboard,"activeClassSkillPresetSlot")+"\nПитомцы: "+BuildAdvisor.val(dashboard,"activePetPresetSlot")+"\nДерево: "+BuildAdvisor.val(dashboard,"activeTreePresetSlot")+"\nСохраняйте разные наборы в игре для босса и фарма, если они доступны.",TEAL);
    }
    @Override public void onDestroy() { destroyed=true; sequence++; executor.shutdownNow(); super.onDestroy(); }
}
