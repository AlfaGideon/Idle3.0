package com.idlearchelper;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.*;

public class OverlayService extends Service {
    private static final String STOP="com.idlearchelper.STOP";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private WindowManager wm; private LinearLayout box, body; private WindowManager.LayoutParams params;
    private TextView footer; private boolean alive; private boolean loading;
    private int dp(float n) { return (int)(getResources().getDisplayMetrics().density*n+.5f); }
    @Override public void onCreate() {
        super.onCreate(); alive=true;
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel("overlay","Плавающее окно",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,OverlayService.class).setAction(STOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification=new Notification.Builder(this,"overlay").setSmallIcon(com.idlearchelper.R.drawable.ic_app).setContentTitle("Idle Arc Helper").setContentText("Плавающее окно включено").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Закрыть",stop).build()).build();
        startForeground(7,notification);
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return; }
        try { createOverlay(); } catch(RuntimeException e) { stopSelf(); }
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if(intent!=null && STOP.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        if(box!=null && !loading) refresh();
        return START_NOT_STICKY;
    }
    private TextView text(String s,int size,int color) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); return t; }
    private void createOverlay() {
        wm=(WindowManager)getSystemService(WINDOW_SERVICE); box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(14),dp(10),dp(14),dp(12));
        GradientDrawable bg=new GradientDrawable(); bg.setColor(0xf21a2436); bg.setCornerRadius(dp(16)); bg.setStroke(dp(1),0xff826947); box.setBackground(bg);
        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL); box.addView(header);
        TextView title=text("⠿  IDLE ARC",13,0xffe1ba7e); title.setTypeface(null,Typeface.BOLD); header.addView(title,new LinearLayout.LayoutParams(0,dp(36),1));
        TextView reload=text("↻",22,Color.WHITE); reload.setPadding(dp(8),0,dp(8),0); header.addView(reload); reload.setOnClickListener(v->refresh());
        TextView close=text("×",24,Color.WHITE); close.setPadding(dp(8),0,0,0); header.addView(close); close.setOnClickListener(v->stopSelf());
        body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); box.addView(body);
        footer=text("Загрузка…",11,0xffa3b0c4); footer.setPadding(0,dp(8),0,0); box.addView(footer);
        params=new WindowManager.LayoutParams(dp(260),WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.START; params.x=dp(20); params.y=dp(100); wm.addView(box,params);
        title.setOnTouchListener(new View.OnTouchListener() { int x,y; float downX,downY;
            @Override public boolean onTouch(View v, android.view.MotionEvent e) { switch(e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: x=params.x; y=params.y; downX=e.getRawX(); downY=e.getRawY(); return true;
                case MotionEvent.ACTION_MOVE: params.x=Math.max(0,x+(int)(e.getRawX()-downX)); params.y=Math.max(0,y+(int)(e.getRawY()-downY)); try { wm.updateViewLayout(box,params); } catch(Exception ignored) {} return true;
                case MotionEvent.ACTION_UP: return true;
                default: return false;
            } }
        });
    }
    private final Runnable next=()->refresh();
    private void refresh() {
        handler.removeCallbacks(next); if(!alive || loading || box==null) return; loading=true; footer.setText("Обновление…");
        executor.execute(()-> { try { Object response=Api.get("/api/dashboard"); handler.post(()-> { if(!alive) return; loading=false; render(response); schedule(); }); }
            catch(Exception e) { String msg=e.getMessage(); handler.post(()-> { if(!alive) return; loading=false; body.removeAllViews(); TextView error=text(msg==null?"Ошибка соединения":msg,12,0xffffb8ae); body.addView(error); footer.setText("Нет актуальных данных"); schedule(); }); } });
    }
    private void render(Object response) {
        body.removeAllViews(); JSONArray pins=MainActivity.pins(this);
        if(pins.length()==0) { body.addView(text("Выберите поля в разделе «Данные персонажа» приложения.",12,0xffc1ccdc)); }
        for(int i=0;i<pins.length();i++) { JSONArray p=pins.optJSONArray(i); if(p==null) continue; Object v=Api.at(response,p); TextView name=text(HumanFormat.labelPath(p),11,0xffaab7c9); name.setPadding(0,dp(6),0,0); body.addView(name); String value=HumanFormat.humanValue(v); if(v instanceof JSONObject || v instanceof JSONArray) value="Поле изменилось — выберите заново"; if(value.length()>100) value=value.substring(0,100)+"…"; TextView line=text(value,15,Color.WHITE); line.setMaxLines(2); line.setTypeface(null,Typeface.BOLD); body.addView(line); }
        footer.setText("Обновлено " + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()));
    }
    private void schedule() { if(alive) handler.postDelayed(next,30000); }
    @Override public void onDestroy() { alive=false; handler.removeCallbacksAndMessages(null); executor.shutdownNow(); if(wm!=null && box!=null) try { wm.removeView(box); } catch(Exception ignored) {} box=null; super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
