package it.compagnovirtuale.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;
    private Button start;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(245, 242, 252));
        getWindow().setNavigationBarColor(Color.rgb(245, 242, 252));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildScreen();
    }

    @Override protected void onResume() { super.onResume(); refreshStatus(); }

    private void buildScreen() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(24), dp(28), dp(24), dp(24));
        page.setBackgroundColor(Color.rgb(245, 242, 252));

        TextView eyebrow = text("IL TUO PICCOLO COMPAGNO", 12, 0xFF7657D5, true);
        page.addView(eyebrow);
        TextView title = text("Sempre qui con te ☀️", 29, 0xFF241E35, true);
        LinearLayout.LayoutParams tp = params(-2, -2); tp.topMargin = dp(10); page.addView(title, tp);
        TextView intro = text("Un pupazzetto che passeggia sullo schermo, ti saluta e ogni tanto ha qualcosa da dirti.", 16, 0xFF625C70, false);
        LinearLayout.LayoutParams ip = params(-1, -2); ip.topMargin = dp(12); page.addView(intro, ip);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        card.setBackground(roundRect(0xFFFFFFFF, dp(24)));
        LinearLayout.LayoutParams cp = params(-1, -2); cp.topMargin = dp(28); page.addView(card, cp);
        TextView mascot = text("🐻", 58, 0xFF241E35, false); mascot.setGravity(Gravity.CENTER);
        card.addView(mascot, params(-1, 86));
        status = text("Il tuo amico sta aspettando di uscire.", 15, 0xFF625C70, false);
        status.setGravity(Gravity.CENTER); LinearLayout.LayoutParams sp = params(-1, -2); sp.topMargin = dp(8); card.addView(status, sp);

        start = button("Fai comparire il pupazzo", 0xFF7657D5, 0xFFFFFFFF);
        LinearLayout.LayoutParams bp = params(-1, 54); bp.topMargin = dp(22); card.addView(start, bp);
        start.setOnClickListener(v -> togglePet());

        Button wave = button("Fagli fare ciao 👋", 0xFFECE6FA, 0xFF493782);
        LinearLayout.LayoutParams wp = params(-1, 50); wp.topMargin = dp(10); card.addView(wave, wp);
        wave.setOnClickListener(v -> send(PetOverlayService.ACTION_WAVE));

        TextView how = text("COME GIOCARE", 12, 0xFF7657D5, true);
        LinearLayout.LayoutParams hp = params(-2, -2); hp.topMargin = dp(28); page.addView(how, hp);
        addTip(page, "✋", "Trascinalo", "Tieni premuto il pupazzetto e spostalo dove vuoi.");
        addTip(page, "👆", "Toccalo", "Risponde al tocco con una nuova animazione e una frase.");
        addTip(page, "💤", "Lascialo tranquillo", "Ogni tanto si siede, fa un saltello o si addormenta.");
        TextView future = text("La conversazione con microfono e voce naturale potrà arrivare in un aggiornamento futuro.", 13, 0xFF817B8E, false);
        LinearLayout.LayoutParams fp = params(-1, -2); fp.topMargin = dp(22); page.addView(future, fp);

        setContentView(page);
        refreshStatus();
    }

    private void togglePet() {
        if (PetOverlayService.isRunning) {
            stopService(new Intent(this, PetOverlayService.class));
        } else if (!Settings.canDrawOverlays(this)) {
            Intent settings = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(settings);
            return;
        } else {
            Intent i = new Intent(this, PetOverlayService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        }
        status.postDelayed(this::refreshStatus, 250);
    }

    private void send(String action) {
        if (!PetOverlayService.isRunning) { togglePet(); status.postDelayed(() -> send(action), 500); return; }
        Intent i = new Intent(this, PetOverlayService.class).setAction(action);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }

    private void refreshStatus() {
        boolean active = PetOverlayService.isRunning;
        if (status != null) status.setText(active ? "È sullo schermo! Toccalo o trascinalo." : "Il tuo amico sta aspettando di uscire.");
        if (start != null) start.setText(active ? "Metti via il pupazzo" : "Fai comparire il pupazzo");
    }

    private void addTip(LinearLayout page, String emoji, String heading, String body) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon = text(emoji, 24, 0xFF241E35, false); row.addView(icon, params(42, 48));
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.addView(text(heading, 15, 0xFF302A3C, true)); TextView desc=text(body, 13, 0xFF716B7D, false); LinearLayout.LayoutParams d=params(-1,-2);d.topMargin=dp(2);col.addView(desc,d);
        LinearLayout.LayoutParams rp=params(-1,-2);rp.topMargin=dp(12);page.addView(row,rp); LinearLayout.LayoutParams cl=params(0,-2,1);cl.leftMargin=dp(8);row.addView(col,cl);
    }
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String s,int bg,int fg){Button b=new Button(this);b.setText(s);b.setTextSize(15);b.setAllCaps(false);b.setTextColor(fg);b.setBackground(roundRect(bg,dp(16)));return b;}
    private android.graphics.drawable.GradientDrawable roundRect(int color,int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
    private LinearLayout.LayoutParams params(int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h));}
    private LinearLayout.LayoutParams params(int w,int h,float weight){return new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h),weight);}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
}
