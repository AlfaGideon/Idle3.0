package com.idlearchelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.*;

/** Optional, user-initiated diagnostic browser. It does not inspect other applications. */
public final class StreamScannerActivity extends Activity {
    private WebView web;
    private final ArrayDeque<String> logs=new ArrayDeque<>();
    private TextView count;
    private int dp(float n) { return (int)(n*getResources().getDisplayMetrics().density+.5f); }
    private void append(String line) {
        if(logs.size()>=100) logs.removeFirst();
        logs.addLast(DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date())+"  "+line);
        if(count!=null) count.setText("Событий: " + logs.size() + "  •  нажмите «Результаты»");
    }
    private String report() {
        StringBuilder b=new StringBuilder("Idle Arc Helper · диагностика потока\n");
        b.append("Только встроенный браузер; служебные адреса частично скрыты.\n");
        for(String s:logs) b.append(s).append('\n');
        return b.toString();
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(8),dp(8),dp(8),dp(8)); root.setBackgroundColor(0xff0b101c); setContentView(root);
        root.setOnApplyWindowInsetsListener((v,i)->{ v.setPadding(dp(8),dp(8)+i.getSystemWindowInsetTop(),dp(8),dp(8)+i.getSystemWindowInsetBottom()); return i; });
        TextView note=new TextView(this); note.setTextColor(Color.WHITE); note.setTextSize(13);
        note.setText("Сканер встроенной веб-версии. Войдите на сайте отдельно, затем поиграйте. Содержимое сообщений, пароли, cookie и токены не записываются."); root.addView(note);
        count=new TextView(this); count.setTextColor(0xffd6ab6e); count.setPadding(0,dp(4),0,dp(4)); root.addView(count);
        if(!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) || !WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            append("Сканер не поддерживается текущей версией Android System WebView. Обновите её и попробуйте снова.");
            Button result=new Button(this); result.setText("Результаты"); result.setOnClickListener(v->showReport()); root.addView(result);
            Button exit=new Button(this); exit.setText("Назад"); exit.setOnClickListener(v->finish()); root.addView(exit); return;
        }
        web=new WebView(this);
        web.getSettings().setJavaScriptEnabled(true); web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false); web.getSettings().setAllowContentAccess(false); web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u=request.getUrl();
                if(isAllowed(u)) return false;
                append("Переход за пределы idlearc.com заблокирован; адрес скрыт.");
                return true;
            }
            @Override public void onReceivedError(WebView v, WebResourceRequest request, WebResourceError error) {
                if(request.isForMainFrame()) append("Ошибка загрузки страницы, код " + error.getErrorCode());
            }
            @Override public void onReceivedSslError(WebView v, SslErrorHandler handler, android.net.http.SslError error) {
                append("Ошибка защищённого соединения, код " + error.getPrimaryError()); handler.cancel();
            }
        });
        Set<String> allowed=new HashSet<>(Arrays.asList("https://idlearc.com", "https://www.idlearc.com"));
        try {
            WebViewCompat.addWebMessageListener(web,"IdleArcStreamProbe",allowed,(view,message,sourceOrigin,isMainFrame,replyProxy)-> {
                if(!isAllowed(sourceOrigin)) return;
                String text=StreamReport.render(message.getData());
                if(text!=null) append(text);
            });
            String script;
            try(InputStream in=getAssets().open("stream_probe.js"); ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
                byte[] buf=new byte[4096]; int n; while((n=in.read(buf))!=-1) bytes.write(buf,0,n);
                script=new String(bytes.toByteArray(),StandardCharsets.UTF_8);
            }
            WebViewCompat.addDocumentStartJavaScript(web,script,allowed);
        } catch(Exception e) {
            append("Не удалось запустить сканер. Обновите Android System WebView."); web.destroy(); web=null;
        }
        if(web!=null) {
            root.addView(web,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
            LinearLayout controls=new LinearLayout(this); root.addView(controls);
            Button result=new Button(this); result.setText("Результаты"); controls.addView(result,new LinearLayout.LayoutParams(0,dp(52),1)); result.setOnClickListener(v->showReport());
            Button exit=new Button(this); exit.setText("Выход"); controls.addView(exit,new LinearLayout.LayoutParams(0,dp(52),1)); exit.setOnClickListener(v->finish());
            append("Откройте игру, войдите на сайте и выполните действия; затем нажмите «Результаты».");
            web.loadUrl("https://idlearc.com/app/");
        } else {
            Button result=new Button(this); result.setText("Результаты"); result.setOnClickListener(v->showReport()); root.addView(result);
            Button exit=new Button(this); exit.setText("Назад"); exit.setOnClickListener(v->finish()); root.addView(exit);
        }
    }
    private static boolean isAllowed(Uri uri) {
        if(uri==null || !"https".equalsIgnoreCase(uri.getScheme())) return false;
        int port=uri.getPort();
        return (port==-1 || port==443) && ("idlearc.com".equalsIgnoreCase(uri.getHost()) || "www.idlearc.com".equalsIgnoreCase(uri.getHost()));
    }
    private void showReport() {
        new AlertDialog.Builder(this).setTitle("Результаты сканирования").setMessage(report()+"\nНеобнаружение потока не доказывает его отсутствие: инструменты не видят все сетевые механизмы и сторонние приложения.")
            .setNegativeButton("Продолжить",null)
            .setNeutralButton("Очистить",(d,w)-> { logs.clear(); append("Журнал очищен."); })
            .setPositiveButton("Скопировать",(d,w)-> { ((ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Диагностика Idle Arc",report())); Toast.makeText(this,"Результаты скопированы",Toast.LENGTH_SHORT).show(); }).show();
    }
    @Override public void onBackPressed() { if(web!=null && web.canGoBack()) web.goBack(); else super.onBackPressed(); }
    @Override public void onDestroy() { if(web!=null) { ((ViewGroup)web.getParent()).removeView(web); web.destroy(); web=null; } super.onDestroy(); }
}
