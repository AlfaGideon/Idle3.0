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
import android.webkit.CookieManager;
import android.text.InputType;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private static final int BG=Color.rgb(11,16,28), PANEL=Color.rgb(25,34,52), TEXT=Color.rgb(235,239,245), MUTED=Color.rgb(156,169,188), GOLD=Color.rgb(221,176,108);
    private LinearLayout root, content, bar;
    private TextView status;
    private boolean loginScreen;
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
        loginScreen = false;
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
        content.addView(button("05   Логи и диагностика",this::showLogs));
        content.addView(button("06   Arcstone Ascent · онлайн",()->startActivity(new Intent(this,LiveAscentActivity.class))));
        content.addView(button("07   Сканирование онлайн-потока",()->startActivity(new Intent(this,StreamScannerActivity.class))));
        content.addView(button("Остановить плавающее окно",()-> { stopService(new Intent(this,OverlayService.class)); toast("Окно остановлено"); }));
        TextView note=text("Окно обновляется каждые 30 секунд, пока включено. Поля выбираются в данных персонажа. Запросы только на чтение.",13,MUTED); note.setPadding(0,dp(10),0,dp(15)); content.addView(note);
        content.addView(button("Выйти из аккаунта",()->new AlertDialog.Builder(this).setTitle("Выйти?").setMessage("Локальный токен и выбранные поля будут удалены, плавающее окно остановится. Серверный токен не отзывается автоматически.").setNegativeButton("Отмена",null).setPositiveButton("Выйти",(d,w)-> { stopService(new Intent(this,OverlayService.class)); getPreferencesFile().edit().clear().apply(); data=null; try { AuthStore.clear(); DiagnosticLog.add("Сессия: локальный токен удалён"); toast("Локальная сессия удалена"); } catch(Exception e) { DiagnosticLog.add("Сессия: ошибка удаления локального токена"); toast("Ошибка удаления сессии. Повторите выход."); } CookieManager.getInstance().removeAllCookies(value -> CookieManager.getInstance().flush()); }).show()));
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
        ++requestId; base("ВХОД В API  •  IDLEARC.COM"); loginScreen=true;
        DiagnosticLog.add("Вход: открыта форма API; Android API " + android.os.Build.VERSION.SDK_INT);
        TextView hint=text("Введите учётные данные IdleArc. Они отправляются по HTTPS непосредственно на idlearc.com/api/login. Пароль и email не сохраняются; токен хранится в защищённом хранилище Android. При истечении токена потребуется войти снова.",14,MUTED); hint.setLineSpacing(dp(2),1); hint.setPadding(0,0,0,dp(18)); content.addView(hint);
        EditText email=new EditText(this); email.setSingleLine(true); email.setTextColor(TEXT); email.setHintTextColor(MUTED); email.setHint("Email IdleArc"); email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); email.setSaveEnabled(false); email.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO); content.addView(email);
        EditText password=new EditText(this); password.setSingleLine(true); password.setTextColor(TEXT); password.setHintTextColor(MUTED); password.setHint("Пароль IdleArc"); password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); password.setSaveEnabled(false); password.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO); content.addView(password);
        TextView feedback=text("",14,MUTED); feedback.setPadding(0,dp(15),0,dp(10)); content.addView(feedback);
        Button submit=button("Войти и проверить данные",()-> {
            String em=email.getText().toString().trim(), pw=password.getText().toString();
            if(em.isEmpty() || pw.isEmpty()) { toast("Укажите email и пароль"); return; }
            password.setText(""); email.setText("");
            final int id=++requestId;
            feedback.setText("Выполняется вход…");
            executor.execute(()-> {
                try {
                    String token=Api.login(em,pw);
                    runOnUiThread(()-> {
                        if(destroyed || id!=requestId) return;
                        try { AuthStore.save(token); DiagnosticLog.add("Сессия: токен сохранён в защищённом хранилище"); load("/api/dashboard"); }
                        catch(Exception e) { DiagnosticLog.add("Сессия: ошибка защищённого хранилища"); feedback.setText("Не удалось безопасно сохранить сессию. Проверьте защиту устройства и повторите вход."); }
                    });
                } catch(Exception e) {
                    String message=e.getMessage();
                    boolean known=message!=null && (message.startsWith("Не удалось войти (HTTP ") || message.startsWith("Ошибка входа: HTTP ") || message.equals("Сервер не вернул корректный токен."));
                    String display=known ? message : "Не удалось выполнить вход. Проверьте подключение и откройте логи.";
                    runOnUiThread(()-> { if(!destroyed && id==requestId) feedback.setText(display); });
                }
            });
        });
        content.addView(submit);
        content.addView(button("Логи и диагностика",this::showLogs)); bar.addView(button("Назад",this::showHome));
    }
    private void load(String ep) {
        endpoint=ep; path=new JSONArray(); page=0; data=null; final int id=++requestId;
        base("ЗАГРУЗКА  •  "+(ep.contains("dashboard")?"ПЕРСОНАЖ":"КОНФИГУРАЦИЯ"));
        content.addView(text("Обращаемся к IdleArc…",16,MUTED)); bar.addView(button("На главную",this::showHome));
        executor.execute(()-> { try { Object result=Api.get(ep); runOnUiThread(()-> { if(!destroyed && id==requestId) { data=result; showNode(); } }); }
            catch(Exception e) { String message=e.getMessage(); runOnUiThread(()-> { if(!destroyed && id==requestId) { content.removeAllViews(); TextView err=text("Не удалось загрузить данные\n\n"+message,16,TEXT); content.addView(err); content.addView(button("Повторить",()->load(ep))); if(ep.contains("dashboard")) content.addView(button("Войти",this::showLogin)); content.addView(button("Открыть логи",this::showLogs)); } }); } });
    }
    private void showLogs() {
        ++requestId; base("ЛОГИ  •  ДИАГНОСТИКА ВХОДА");
        TextView hint=text("Повторите попытку входа, затем скопируйте логи и отправьте их разработчику. Пароли, токены, адреса с параметрами и ответы API не записываются. Логи хранятся только в памяти до закрытия процесса приложения.",13,MUTED); hint.setPadding(0,0,0,dp(14)); content.addView(hint);
        TextView log=text(DiagnosticLog.snapshot(),13,TEXT); log.setTextIsSelectable(true); log.setPadding(dp(12),dp(12),dp(12),dp(12)); log.setBackground(shape(PANEL,12)); content.addView(log);
        bar.addView(button("Скопировать логи",()-> { android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE); clipboard.setPrimaryClip(ClipData.newPlainText("Idle Arc Helper — логи", DiagnosticLog.snapshot())); toast("Логи скопированы"); }));
        bar.addView(button("Обновить логи",this::showLogs));
        bar.addView(button("Очистить",()-> { DiagnosticLog.clear(); showLogs(); }));
        bar.addView(button("На главную",this::showHome));
    }
    private JSONArray childPath(Object key) { JSONArray p=new JSONArray(); for(int i=0;i<path.length();i++) p.put(path.opt(i)); p.put(key); return p; }
    private void showNode() {
        base((endpoint.contains("dashboard")?"ПЕРСОНАЖ":"КОНФИГУРАЦИЯ")+"  •  "+(path.length()==0?"ОБЗОР":HumanFormat.labelPath(path)));
        Object node=Api.at(data,path);
        TextView hint=text(endpoint.contains("dashboard")?"Откройте раздел или нажмите на значение, чтобы увидеть подробности и закрепить его в окне.":"Откройте раздел или нажмите на значение, чтобы увидеть подробности. Изменение на сервере не поддерживается.",13,MUTED); hint.setPadding(0,0,0,dp(14)); content.addView(hint);
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
        String summary=group ? (value instanceof JSONObject ? ((JSONObject)value).length()+" полей" : ((JSONArray)value).length()+" элементов")+"  ›" : HumanFormat.humanValue(key,value);
        if(summary.length()>160) summary=summary.substring(0,160)+"…";
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(dp(15),dp(12),dp(15),dp(12)); row.setBackground(shape(PANEL,12));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=dp(7); content.addView(row,lp);
        String label=key.startsWith("#") ? "Элемент " + key : HumanFormat.label(key);
        TextView name=text(label+(endpoint.contains("dashboard")&&!group&&pinned(target)?"  ★":""),14,GOLD); name.setTypeface(null,Typeface.BOLD); row.addView(name);
        TextView val=text(summary,15,TEXT); val.setPadding(0,dp(4),0,0); row.addView(val);
        row.setOnClickListener(v->{ if(group) { path=target; page=0; showNode(); } else showField(key,value,target); });
    }
    private void showField(String key, Object value, JSONArray target) {
        String title=key.startsWith("#") ? "Элемент " + key : HumanFormat.label(key);
        String detail="Поле: " + Api.label(target) + "\nТип: " + HumanFormat.type(value)
                + "\n\nЗначение: " + HumanFormat.humanValue(key,value)
                + "\n\nИсходное значение: " + HumanFormat.jsonValue(value)
                + "\n\nТолько просмотр: API изменения этого параметра не предоставлен.";
        AlertDialog.Builder dialog=new AlertDialog.Builder(this).setTitle(title).setMessage(detail).setNegativeButton("Закрыть",null);
        if(endpoint.contains("dashboard")) dialog.setPositiveButton(pinned(target)?"Убрать из окна":"В плавающее окно",(d,w)->togglePin(target));
        dialog.show();
    }
    private void startOverlay() {
        if(!Settings.canDrawOverlays(this)) { toast("Разрешите отображение поверх других приложений, затем нажмите кнопку ещё раз"); startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()))); return; }
        if(android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        try { startForegroundService(new Intent(this,OverlayService.class)); toast("Плавающее окно запущено"); } catch(Exception e) { toast("Не удалось запустить окно: "+e.getMessage()); }
    }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
    @Override public void onBackPressed() { if(loginScreen) showHome(); else if(data!=null && path.length()>0) { JSONArray p=new JSONArray(); for(int i=0;i<path.length()-1;i++) p.put(path.opt(i)); path=p; page=0; showNode(); } else showHome(); }
    @Override public void onDestroy() { destroyed=true; ++requestId; executor.shutdownNow(); super.onDestroy(); }
}
