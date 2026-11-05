package it.compagnovirtuale.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import java.util.Random;

public class PetOverlayService extends Service {
    static final String ACTION_WAVE = "it.compagnovirtuale.app.WAVE";
    private static final String ACTION_STOP = "it.compagnovirtuale.app.STOP";
    private static final String CHANNEL = "compagnovirtuale_pet";
    static volatile boolean isRunning = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private WindowManager windowManager;
    private WindowManager.LayoutParams layout;
    private PetView pet;
    private int screenW, screenH, minY, maxY;
    private float density, phase, vx = 1.3f;
    private int mood = 0; // 0 passeggiata, 1 saluto, 2 saltello, 3 seduto, 4 nanna
    private long moodUntil, nextThought, bubbleUntil;
    private String thought = "Ciao! Sono felice di essere qui 🐻";
    private boolean dragging;
    private float downX, downY, startX, startY;
    private final Runnable frame = new Runnable() {
        @Override public void run() {
            if (pet == null) return;
            long now = System.currentTimeMillis();
            phase += 0.12f;
            if (!dragging && mood == 0) {
                layout.x += Math.round(vx * density);
                int maxX = Math.max(0, screenW - Math.round(155 * density));
                if (layout.x <= 0) { layout.x = 0; vx = Math.abs(vx); }
                else if (layout.x >= maxX) { layout.x = maxX; vx = -Math.abs(vx); }
            }
            if (mood != 0 && now >= moodUntil) mood = 0;
            if (now >= nextThought) {
                say(randomThought());
                nextThought = now + (35_000 + random.nextInt(35_000));
            }
            if (bubbleUntil > 0 && now >= bubbleUntil) { thought = null; bubbleUntil = 0; }
            try { windowManager.updateViewLayout(pet, layout); } catch (Exception ignored) { }
            pet.invalidate();
            handler.postDelayed(this, 130);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(dm);
        density = dm.density; screenW = dm.widthPixels; screenH = dm.heightPixels;
        minY = Math.round(30 * density); maxY = Math.max(minY, screenH - Math.round(240 * density));
        createChannel();
        startForeground(41, notification());
        pet = new PetView();
        layout = new WindowManager.LayoutParams(Math.round(165 * density), Math.round(220 * density),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT);
        layout.gravity = Gravity.TOP | Gravity.START;
        layout.x = Math.max(0, screenW - Math.round(175 * density));
        layout.y = Math.min(maxY, Math.max(minY, screenH / 3));
        try { windowManager.addView(pet, layout); isRunning = true; }
        catch (Exception e) { stopSelf(); return; }
        long now = System.currentTimeMillis(); nextThought = now + 18_000 + random.nextInt(15_000); bubbleUntil = now + 5_000;
        handler.postDelayed(frame, 130);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        if (intent != null && ACTION_WAVE.equals(intent.getAction()) && pet != null) {
            setMood(1, 3200); say("Ehi, ciao a te! 👋");
        }
        return START_STICKY;
    }

    private void setMood(int value, long duration) { mood = value; moodUntil = System.currentTimeMillis() + duration; }
    private void say(String value) { thought = value; bubbleUntil = System.currentTimeMillis() + 5200; if (pet != null) pet.invalidate(); }
    private String randomThought() {
        String[] lines = {
            "Come va la giornata? 🌼", "Facciamo una pausetta? ☕", "Ti mando un abbraccio! 🤗",
            "Psst… sei una bella persona ✨", "Ho fatto un saltello per te!", "Qui c'è un posticino al sole ☀️",
            "Promemoria: fai un bel respiro 🌿", "Sei ancora lì? Io sì! 🐻", "Oggi hai già sorriso? 🙂",
            "Mi siedo qui vicino a te." };
        return lines[random.nextInt(lines.length)];
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Compagno sullo schermo", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Mantiene visibile il tuo pupazzetto animato");
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
    }
    private Notification notification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, PetOverlayService.class).setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 1, stop, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_menu_info_details).setContentTitle("Il tuo compagno è qui")
                .setContentText("Toccalo per un saluto · trascinalo per spostarlo")
                .setContentIntent(content).setOngoing(true).addAction(android.R.drawable.ic_menu_close_clear_cancel, "Nascondi", stopPending).build();
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(frame);
        if (pet != null) { try { windowManager.removeView(pet); } catch (Exception ignored) { } pet = null; }
        isRunning = false; super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }

    private final class PetView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private long pressAt;
        private boolean longPressed;
        PetView() { super(PetOverlayService.this); setLayerType(View.LAYER_TYPE_HARDWARE, null); }

        @Override public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX=e.getRawX(); downY=e.getRawY(); startX=layout.x; startY=layout.y;
                    pressAt=System.currentTimeMillis(); longPressed=false; dragging=false;
                    handler.postDelayed(() -> {
                        if (downX >= 0 && !dragging && System.currentTimeMillis()-pressAt >= 520) {
                            longPressed=true; setMood(4, 8000); say("Faccio un pisolino qui vicino… 💤");
                        }
                    }, 540);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx=e.getRawX()-downX, dy=e.getRawY()-downY;
                    if (Math.abs(dx)+Math.abs(dy)>12*density) dragging=true;
                    if (dragging) {
                        layout.x=Math.max(0,Math.min(screenW-getWidth(),Math.round(startX+dx)));
                        layout.y=Math.max(minY,Math.min(maxY,Math.round(startY+dy)));
                        try { windowManager.updateViewLayout(this,layout); } catch(Exception ignored) { }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!dragging && !longPressed && System.currentTimeMillis()-pressAt < 500) {
                        int pick=random.nextInt(3);
                        if(pick==0){setMood(1,2600);say("Ehi, ciao a te! 👋");}
                        else if(pick==1){setMood(2,2000);say("Iuuup! Un saltello! ✨");}
                        else {setMood(3,3500);say("Mi siedo un attimo con te 💛");}
                    }
                    downX=-1; dragging=false; return true;
            }
            return true;
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c); c.save(); c.scale(density,density);
            float bounce=(mood==2 ? (float)Math.abs(Math.sin(phase*2.6))*17 : mood==0 ? (float)Math.sin(phase)*2 : 0);
            float cx=82, base=202-bounce;
            if(thought!=null && System.currentTimeMillis()<bubbleUntil) drawBubble(c, thought);
            // ears
            fill(0xFFD99A63); c.drawCircle(cx-25,base-93,17,p);c.drawCircle(cx+25,base-93,17,p);
            fill(0xFFFFD8C0);c.drawCircle(cx-25,base-93,8,p);c.drawCircle(cx+25,base-93,8,p);
            // body and feet
            fill(0xFFB97745); r.set(cx-34,base-63,cx+34,base-7);c.drawOval(r,p);
            fill(0xFF8D5739);r.set(cx-35,base-24,cx-3,base);c.drawOval(r,p);r.set(cx+3,base-24,cx+35,base);c.drawOval(r,p);
            // little arms
            fill(0xFFD99A63); c.save();
            float wave=(mood==1 ? (float)Math.sin(phase*5)*28 : mood==0 ? (float)Math.sin(phase)*8 : 0);
            c.rotate(-28-wave,cx-28,base-52);r.set(cx-48,base-64,cx-25,base-30);c.drawRoundRect(r,11,11,p);
            c.restore();r.set(cx+24,base-62,cx+48,base-30);c.drawRoundRect(r,11,11,p);
            // face
            fill(0xFFFFD9B6);r.set(cx-24,base-91,cx+24,base-48);c.drawOval(r,p);
            fill(0xFF38271F);float eyeY=base-72;
            if(mood==4 || ((int)(phase*3)%29==0)) { p.setColor(0xFF38271F);p.setStrokeWidth(2);c.drawLine(cx-14,eyeY,cx-7,eyeY,p);c.drawLine(cx+7,eyeY,cx+14,eyeY,p); }
            else {c.drawCircle(cx-10,eyeY,2.5f,p);c.drawCircle(cx+10,eyeY,2.5f,p);}
            fill(0xFF9B5949);c.drawCircle(cx,base-63,3,p);
            fill(0xFFFFB5B5);c.drawCircle(cx-18,base-63,4,p);c.drawCircle(cx+18,base-63,4,p);
            // belly patch
            fill(0xFFFFD9B6);r.set(cx-17,base-32,cx+17,base-10);c.drawOval(r,p);
            if(mood==4){text(c,"z",cx+39,base-93,14,0xFF7657D5,true);text(c,"z",cx+48,base-109,11,0xFF7657D5,true);}
            c.restore();
        }
        private void drawBubble(Canvas c,String s){
            float left=5,top=5,right=160,bottom=53;
            fill(Color.WHITE);r.set(left,top,right,bottom);c.drawRoundRect(r,16,16,p);
            Path tail=new Path();tail.moveTo(72,51);tail.lineTo(82,61);tail.lineTo(91,51);c.drawPath(tail,p);
            p.setColor(0xFF382F45);p.setTypeface(TypefaceHolder.BOLD);p.setTextSize(11);
            String display=s; if(p.measureText(display)>142){while(display.length()>4 && p.measureText(display+"…")>142)display=display.substring(0,display.length()-1);display+="…";}
            c.drawText(display,12,34,p);
        }
        private void fill(int color){p.setColor(color);p.setStyle(Paint.Style.FILL);p.setStrokeWidth(1);}
        private void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){p.setColor(color);p.setTextSize(size);p.setTypeface(bold?TypefaceHolder.BOLD:TypefaceHolder.NORMAL);c.drawText(s,x,y,p);}
    }
    private static final class TypefaceHolder {
        static final android.graphics.Typeface NORMAL=android.graphics.Typeface.create("sans-serif",0);
        static final android.graphics.Typeface BOLD=android.graphics.Typeface.create("sans-serif",1);
    }
}
