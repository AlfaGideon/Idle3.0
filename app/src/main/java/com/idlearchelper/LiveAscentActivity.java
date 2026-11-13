package com.idlearchelper;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/** Live display of the observed public Ascent event, using the official page's connection. */
public final class LiveAscentActivity extends Activity {
    private WebView web;
    private TextView status, values;
    private boolean foreground, hasValue;
    private int dp(int n) { return (int)(getResources().getDisplayMetrics().density*n+.5f); }
    private TextView text(String value,int size,int color) { TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); return t; }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(12),dp(12),dp(12),dp(8)); root.setBackgroundColor(0xff0b101c); setContentView(root);
        root.setOnApplyWindowInsetsListener((v,i)-> { v.setPadding(dp(12),dp(12)+i.getSystemWindowInsetTop(),dp(12),dp(8)+i.getSystemWindowInsetBottom()); return i; });
        TextView title=text("ARCSTONE ASCENT  ·  ОНЛАЙН",17,0xffe1ba7e); title.setTypeface(null,Typeface.BOLD); root.addView(title);
        status=text("Открываем сайт…",12,0xffadbdcf); status.setPadding(0,dp(4),0,dp(4)); root.addView(status);
        values=text("Общий пул: —    •    Уровень пула: —\nПереполнение: —    •    Участники: —\nУчастники за всё время: —",14,Color.WHITE); values.setPadding(0,dp(4),0,dp(7)); root.addView(values);
        TextView hint=text("Общий игровой пул, не баланс вашего аккаунта. События приходят только пока этот экран открыт и сайт подключён. При необходимости войдите в игру ниже.",12,0xffadbdcf); hint.setPadding(0,0,0,dp(6)); root.addView(hint);
        if(!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) || !WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            status.setText("Обновите Android System WebView: поток в этой версии не поддерживается.");
            Button back=new Button(this); back.setText("Назад"); back.setOnClickListener(v->finish()); root.addView(back); return;
        }
        web=new WebView(this);
        web.getSettings().setJavaScriptEnabled(true); web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false); web.getSettings().setAllowContentAccess(false); web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return !isAllowed(request.getUrl()); }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) { if(request.isForMainFrame()) stale("Не удалось открыть страницу (код " + error.getErrorCode() + ")"); }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, android.net.http.SslError error) { stale("Ошибка защищённого соединения"); handler.cancel(); }
        });
        Set<String> origins=new HashSet<>(Arrays.asList("https://idlearc.com","https://www.idlearc.com"));
        try {
            WebViewCompat.addWebMessageListener(web,"IdleArcAscent",origins,(view,message,sourceOrigin,isMainFrame,reply)->{
                if(!foreground || !isAllowed(sourceOrigin)) return;
                AscentEvent event=AscentEvent.parse(message.getData());
                if(event!=null) receive(event);
            });
            String script;
            try(InputStream in=getAssets().open("ascent_live.js"); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] buffer=new byte[4096]; int n; while((n=in.read(buffer))!=-1) out.write(buffer,0,n);
                script=new String(out.toByteArray(),StandardCharsets.UTF_8);
            }
            WebViewCompat.addDocumentStartJavaScript(web,script,origins);
            root.addView(web,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
            LinearLayout controls=new LinearLayout(this); root.addView(controls);
            Button retry=new Button(this); retry.setText("Перезагрузить игру"); controls.addView(retry,new LinearLayout.LayoutParams(0,dp(50),1));
            retry.setOnClickListener(v->{ stale("Переподключаемся…"); web.loadUrl("https://idlearc.com/app/"); });
            Button back=new Button(this); back.setText("Назад"); controls.addView(back,new LinearLayout.LayoutParams(0,dp(50),1)); back.setOnClickListener(v->finish());
            web.loadUrl("https://idlearc.com/app/");
        } catch(Exception e) { status.setText("Не удалось запустить онлайн-поток. Обновите Android System WebView."); web.destroy(); web=null; Button back=new Button(this); back.setText("Назад"); back.setOnClickListener(v->finish()); root.addView(back); }
    }
    private static boolean isAllowed(Uri uri) {
        if(uri==null || !"https".equalsIgnoreCase(uri.getScheme())) return false;
        int port=uri.getPort(); return (port==-1 || port==443) && ("idlearc.com".equalsIgnoreCase(uri.getHost()) || "www.idlearc.com".equalsIgnoreCase(uri.getHost()));
    }
    private static String show(String s) { return s==null?"н/д":s; }
    private void stale(String why) { status.setText(why+(hasValue?" · последние данные могут устареть":"")); }
    private void receive(AscentEvent event) {
        switch(event.kind) {
            case "connecting": stale("Подключение к потоку…"); break;
            case "open": stale("Соединение открыто · ожидаем подписку"); break;
            case "subscribed": stale("Канал подключён · ожидаем обновление"); break;
            case "closed": stale("Соединение закрыто · откройте игру снова"); break;
            case "error": stale("Ошибка соединения · попробуйте перезагрузить игру"); break;
            case "ascent":
                hasValue=true;
                values.setText("Общий пул: "+show(event.pool)+"    •    Уровень пула: "+show(event.tier)
                    +"\nПереполнение: "+show(event.overflow)+(event.overflow==null?"":"%")+"    •    Участники: "+show(event.contributors)
                    +"\nУчастники за всё время: "+show(event.lifetime));
                status.setText("Получено по WebSocket в "+DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date()));
                break;
        }
    }
    @Override public void onResume() { super.onResume(); foreground=true; if(web!=null) web.onResume(); if(hasValue) stale("Ожидаем новое событие после возвращения"); }
    @Override public void onPause() { foreground=false; if(web!=null) web.onPause(); super.onPause(); }
    @Override public void onBackPressed() { if(web!=null && web.canGoBack()) web.goBack(); else super.onBackPressed(); }
    @Override public void onDestroy() { if(web!=null) { if(web.getParent() instanceof ViewGroup) ((ViewGroup)web.getParent()).removeView(web); web.destroy(); web=null; } super.onDestroy(); }
}
