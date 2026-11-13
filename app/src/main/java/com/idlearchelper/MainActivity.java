package com.idlearchelper;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private static final int BG=Color.rgb(11,16,28), PANEL=Color.rgb(25,34,52), TEXT=Color.rgb(235,239,245), MUTED=Color.rgb(156,169,188), GOLD=Color.rgb(221,176,108);
    private LinearLayout root, content, bar;
    private TextView status;
    private WebView login;
    private Object data;
    private JSONArray path = new JSONArray();
    private String endpoint = "/api/dashboard";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private int requestId;
    private boolean destroyed;
    private int page;

    @Override public void onCreate(Bundle b) { super.onCreate(b); getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG); showHome(); }
    private int dp(float x) { return (int)(getResources().getDisplayMetrics().density*x+.5f); }
    private GradientDrawable shape(int color, int radius) { GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }
    private TextView text(String s, int size, int color) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); return t; }
    private Button button(String s, Runnable action) { Button b=new Button(this); b.setText(s); b.setTextColor(TEXT); b.setAllCaps(false); b.setBackground(shape(PANEL,12)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48)); p.bottomMargin=dp(8); b.setLayoutParams(p); b.setOnClickListener(v->action.run()); return b; }
    private void base(String subtitle) {
        if (login != null) { login.destroy(); login=null; }
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20),dp(20),dp(20),dp(12)); root.setBackgroundColor(BG); setContentView(root);
        root.setOnApplyWindowInsetsListener((v, insets) -> { v.setPadding(dp(20),dp(20)+insets.getSystemWindowInsetTop(),dp(20),dp(12)+insets.getSystemWindowInsetBottom()); return insets; });
        TextView eyebrow=text("IDLE ARC  /  COMPANION",11,GOLD); eyebrow.setTypeface(null,Typeface.BOLD); root.addView(eyebrow);
        TextView title=text("Idle Arc Helper",28,TEXT); title.setTypeface(null,Typeface.BOLD); root.addView(title);
        status=text(subtitle,13,MUTED); status.setPadding(0,dp(4),0,dp(16)); root.addView(status);
        ScrollView scroll=new ScrollView(this); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); scroll.addView(content);
        bar=new LinearLayout(this); bar.setOrientation(LinearLayout.VERTICAL); root.addView(bar);
    }
    private void showHome() {
        ++requestId; base("ВАШИ ДАННЫЕ  •  БЫСТРЫЙ ДОСТУП");
        TextView hero=text("Всё важное —\nповерх игры.",27,TEXT); hero.setTypeface(null,Typeface.BOLD); hero.setPadding(0,dp(10),0,dp(8)); content.addView(hero);
        TextView desc=text("Войдите в IdleArc, откройте данные персонажа и закрепите нужные показатели в плавающей панели.",15,MUTED); desc.setLineSpacing(dp(3),1); desc.setPadding(0,0,0,dp(24)); content.addView(desc);
        content.addView(button("01   Войти в IdleArc",this::showLogin));
        content.addView(button("02   Данные персонажа",()->load("/api/dashboard")));
        content.addView(button("03   Конфигурация игры",()->load("/api/config/game")));
        content.addView(button("04   Запустить плавающее окно",this::startOverlay));
        content.addView(button("Остановить плавающее окно",()-> { stopService(new Intent(this,OverlayService.class)); toast("Окно остановлено"); }));
        TextView note=text("Окно обновляется каждые 30 секунд, пока включено. Поля выбираются в данных персонажа. Запросы только на чтение.",13,MUTED); note.setPadding(0,dp(10),0,dp(15)); content.addView(note);
        content.addView(button("Выйти из аккаунта",()->new AlertDialog.Builder(this).setTitle("Выйти?").setMessage("Локальные cookies IdleArc и выбранные поля будут удалены, плавающее окно остановится.").setNegativeButton("Отмена",null).setPositiveButton("Выйти",(d,w)-> { stopService(new Intent(this,OverlayService.class)); getPreferencesFile().edit().clear().apply(); CookieManager.getInstance().removeAllCookies(value -> CookieManager.getInstance().flush()); toast("Локальная сессия удалена"); }).show()));
    }
    static android.content.SharedPreferences prefs(Context c) { return c.getSharedPreferences("pins",MODE_PRIVATE); }
    private android.content.SharedPreferences getPreferencesFile() { return prefs(this); }
    static JSONArray pins(Context c) { try { return new JSONArray(prefs(c).getString("paths","[]")); } catch(Exception e) { return new JSONArray(); } }
    private void togglePin(JSONArray p) {
        JSONArray old=pins(this), next=new JSONArray(); boolean exists=false;
        for(int i=0;i<old.length();i++) { JSONArray x=old.optJSONArray(i); if(x!=null) { if(x.toString().equals(p.toString())) exists=true; else next.put(x); } }
        if(!exists) { if(next.length()>=8) { toast("Можно закрепить до 8 полей"); return; } next.put(p); }
        prefs(this).edit().putString("paths",next.toString()).apply(); toast(exists?"Удалено из окна":"Добавлено в окно"); showNode();
    }
    private boolean pinned(JSONArray p) { JSONArray a=pins(this); for(int i=0;i<a.length();i++) if(p.toString().equals(String.valueOf(a.opt(i)))) return true; return false; }
    private void showLogin() {
        ++requestId; base("БЕЗОПАСНЫЙ ВХОД  •  IDLEARC.COM");
        TextView hint=text("Вход выполняется на официальном сайте. После входа нажмите «Готово». Пароль приложению не передаётся.",14,MUTED); hint.setPadding(0,0,0,dp(10)); content.addView(hint);
        // WebView must be visible and given a fixed height inside the scroll container.
        login=new WebView(this); login.getSettings().setJavaScriptEnabled(true); login.getSettings().setDomStorageEnabled(true); login.getSettings().setAllowFileAccess(false); login.getSettings().setAllowContentAccess(false); login.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(login,false);
        login.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest req) {
                Uri u=req.getUrl(); if("https".equals(u.getScheme()) && ("idlearc.com".equalsIgnoreCase(u.getHost()) || "www.idlearc.com".equalsIgnoreCase(u.getHost()))) return false;
                if ("https".equals(u.getScheme())) { try { startActivity(new Intent(Intent.ACTION_VIEW,u)); } catch(Exception ignored) {} } return true;
            }
        });
        content.addView(login,new LinearLayout.LayoutParams(-1,dp(520))); login.loadUrl(Api.BASE+"/login");
        bar.addView(button("Готово · проверить данные",()-> { CookieManager.getInstance().flush(); load("/api/dashboard"); })); bar.addView(button("Назад",this::showHome));
    }
    private void load(String ep) {
        endpoint=ep; path=new JSONArray(); page=0; data=null; final int id=++requestId;
        base("ЗАГРУЗКА  •  "+(ep.contains("dashboard")?"ПЕРСОНАЖ":"КОНФИГУРАЦИЯ"));
        content.addView(text("Обращаемся к IdleArc…",16,MUTED)); bar.addView(button("На главную",this::showHome));
        executor.execute(()-> { try { Object result=Api.get(ep); runOnUiThread(()-> { if(!destroyed && id==requestId) { data=result; showNode(); } }); }
            catch(Exception e) { String message=e.getMessage(); runOnUiThread(()-> { if(!destroyed && id==requestId) { content.removeAllViews(); TextView err=text("Не удалось загрузить данные\n\n"+message,16,TEXT); content.addView(err); content.addView(button("Повторить",()->load(ep))); if(ep.contains("dashboard")) content.addView(button("Войти",this::showLogin)); } }); } });
    }
    private JSONArray childPath(Object key) { JSONArray p=new JSONArray(); for(int i=0;i<path.length();i++) p.put(path.opt(i)); p.put(key); return p; }
    private void showNode() {
        base((endpoint.contains("dashboard")?"ПЕРСОНАЖ":"КОНФИГУРАЦИЯ")+"  •  "+(path.length()==0?"ОБЗОР":Api.label(path)));
        Object node=Api.at(data,path);
        TextView hint=text(endpoint.contains("dashboard")?"Откройте раздел. Нажмите на значение, чтобы добавить его в окно (до 8 полей).":"Откройте раздел, чтобы изучить значения игровой конфигурации.",13,MUTED); hint.setPadding(0,0,0,dp(14)); content.addView(hint);
        if(node instanceof JSONObject) {
            JSONObject obj=(JSONObject)node; List<String> keys=new ArrayList<>(); Iterator<String> it=obj.keys(); while(it.hasNext()) keys.add(it.next()); Collections.sort(keys,String.CASE_INSENSITIVE_ORDER);
            if(keys.isEmpty()) content.addView(text("Нет данных",15,MUTED));
            int start=page*100, end=Math.min(start+100,keys.size());
            for(int i=start;i<end;i++) { String key=keys.get(i); addRow(key,obj.opt(key),childPath(key)); }
            pagination(keys.size());
        } else if(node instanceof JSONArray) {
            JSONArray a=(JSONArray)node; if(a.length()==0) content.addView(text("Нет данных",15,MUTED));
            int start=page*100, end=Math.min(start+100,a.length());
            for(int i=start;i<end;i++) addRow("#"+(i+1),a.opt(i),childPath(i));
            pagination(a.length());
        } else content.addView(text(Api.value(node),16,TEXT));
        if(path.length()>0) bar.addView(button("← Назад",()-> { JSONArray p=new JSONArray(); for(int i=0;i<path.length()-1;i++) p.put(path.opt(i)); path=p; page=0; showNode(); }));
        bar.addView(button("Обновить",()->load(endpoint))); bar.addView(button("На главную",this::showHome));
    }
    private void pagination(int count) {
        if(count<=100) return;
        content.addView(text("Страница "+(page+1)+" / "+((count+99)/100),13,MUTED));
        if(page>0) content.addView(button("← Предыдущая страница",()-> { page--; showNode(); }));
        if((page+1)*100<count) content.addView(button("Следующая страница →",()-> { page++; showNode(); }));
    }
    private void addRow(String key,Object value,JSONArray target) {
        boolean group=value instanceof JSONObject || value instanceof JSONArray;
        String summary=group ? (value instanceof JSONObject ? ((JSONObject)value).length()+" полей" : ((JSONArray)value).length()+" элементов")+"  ›" : Api.value(value);
        if(summary.length()>160) summary=summary.substring(0,160)+"…";
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(dp(15),dp(12),dp(15),dp(12)); row.setBackground(shape(PANEL,12));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=dp(7); content.addView(row,lp);
        TextView name=text(key.replace('_',' ')+(endpoint.contains("dashboard")&&!group&&pinned(target)?"  ★":""),14,GOLD); name.setTypeface(null,Typeface.BOLD); row.addView(name); TextView val=text(summary,15,TEXT); val.setPadding(0,dp(4),0,0); row.addView(val);
        row.setOnClickListener(v->{ if(group) { path=target; page=0; showNode(); } else if(endpoint.contains("dashboard")) togglePin(target); });
    }
    private void startOverlay() {
        if(!Settings.canDrawOverlays(this)) { toast("Разрешите отображение поверх других приложений, затем нажмите кнопку ещё раз"); startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()))); return; }
        if(android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        try { startForegroundService(new Intent(this,OverlayService.class)); toast("Плавающее окно запущено"); } catch(Exception e) { toast("Не удалось запустить окно: "+e.getMessage()); }
    }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
    @Override public void onBackPressed() { if(login!=null && login.canGoBack()) login.goBack(); else if(data!=null && path.length()>0) { JSONArray p=new JSONArray(); for(int i=0;i<path.length()-1;i++) p.put(path.opt(i)); path=p; page=0; showNode(); } else showHome(); }
    @Override public void onDestroy() { destroyed=true; ++requestId; if(login!=null) login.destroy(); executor.shutdownNow(); super.onDestroy(); }
}
