package com.voidl.launcher;

import android.app.*; import android.content.*; import android.content.pm.*;
import android.graphics.*; import android.os.Bundle; import android.view.*; import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
  SharedPreferences sp; VoidBg bg; GridLayout home; LinearLayout drawer; FrameLayout root;
  List<ResolveInfo> apps; PackageManager pm;

  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    sp = getSharedPreferences("void", 0); pm = getPackageManager();
    root = new FrameLayout(this);
    bg = new VoidBg(this, sp.getInt("wp", 0)); root.addView(bg);

    LinearLayout main = new LinearLayout(this); main.setOrientation(LinearLayout.VERTICAL);
    main.setPadding(dp(16), dp(48), dp(16), dp(24));
    TextView clock = new TextClock(this); ((TextClock)clock).setFormat24Hour("HH:mm");
    clock.setTextColor(0xFFE9D5FF); clock.setTextSize(64); clock.setGravity(Gravity.CENTER);
    clock.setShadowLayer(30, 0, 0, 0xFFA855F7); main.addView(clock);
    home = new GridLayout(this); home.setColumnCount(4);
    main.addView(home, new LinearLayout.LayoutParams(-1, 0, 1));
    LinearLayout bar = new LinearLayout(this); bar.setGravity(Gravity.CENTER);
    bar.addView(btn("⬡ Apps", v -> showDrawer(true)));
    bar.addView(btn("✦ Fondo", v -> { int w = (sp.getInt("wp",0)+1) % VoidBg.N; sp.edit().putInt("wp", w).apply(); bg.set(w); }));
    main.addView(bar);
    root.addView(main);

    ScrollView sc = new ScrollView(this);
    drawer = new LinearLayout(this); drawer.setOrientation(LinearLayout.VERTICAL);
    drawer.setPadding(dp(16), dp(48), dp(16), dp(16)); sc.addView(drawer);
    sc.setBackgroundColor(0xE60A0014); sc.setVisibility(View.GONE); sc.setTag("d");
    root.addView(sc);
    setContentView(root);
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
  }

  @Override protected void onResume() { super.onResume(); load(); }

  void load() {
    Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
    apps = pm.queryIntentActivities(i, 0);
    apps.sort((a, c) -> a.loadLabel(pm).toString().compareToIgnoreCase(c.loadLabel(pm).toString()));
    drawer.removeAllViews();
    TextView t = new TextView(this); t.setText("Mantén pulsado para fijar en inicio");
    t.setTextColor(0xFFC084FC); t.setPadding(0,0,0,dp(12)); drawer.addView(t);
    for (ResolveInfo r : apps) drawer.addView(row(r));
    home.removeAllViews();
    for (String p : pins()) for (ResolveInfo r : apps)
      if (r.activityInfo.packageName.equals(p)) { home.addView(cell(r)); break; }
  }

  List<String> pins() { String s = sp.getString("pins", ""); return s.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(s.split(","))); }
  void savePins(List<String> l) { sp.edit().putString("pins", String.join(",", l)).apply(); load(); }

  View row(ResolveInfo r) {
    LinearLayout l = new LinearLayout(this); l.setGravity(Gravity.CENTER_VERTICAL); l.setPadding(0, dp(6), 0, dp(6));
    ImageView iv = new ImageView(this); iv.setImageDrawable(r.loadIcon(pm)); l.addView(iv, dp(44), dp(44));
    TextView tv = new TextView(this); tv.setText(r.loadLabel(pm)); tv.setTextColor(0xFFF3E8FF); tv.setTextSize(16); tv.setPadding(dp(14),0,0,0); l.addView(tv);
    String p = r.activityInfo.packageName;
    l.setOnClickListener(v -> launch(p));
    l.setOnLongClickListener(v -> { List<String> ps = pins();
      if (ps.contains(p)) toast("Ya está en inicio"); else if (ps.size() >= 20) toast("Inicio lleno");
      else { ps.add(p); savePins(ps); toast("Fijada ✦"); } return true; });
    return l;
  }

  View cell(ResolveInfo r) {
    LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setGravity(Gravity.CENTER);
    l.setPadding(dp(4), dp(10), dp(4), dp(10));
    ImageView iv = new ImageView(this); iv.setImageDrawable(r.loadIcon(pm));
    iv.setBackground(glow()); iv.setPadding(dp(8),dp(8),dp(8),dp(8)); l.addView(iv, dp(60), dp(60));
    TextView tv = new TextView(this); tv.setText(r.loadLabel(pm)); tv.setTextColor(0xFFE9D5FF); tv.setTextSize(11);
    tv.setSingleLine(); tv.setGravity(Gravity.CENTER); l.addView(tv);
    GridLayout.LayoutParams lp = new GridLayout.LayoutParams(); lp.width = 0;
    lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); l.setLayoutParams(lp);
    String p = r.activityInfo.packageName;
    l.setOnClickListener(v -> launch(p));
    l.setOnLongClickListener(v -> { new AlertDialog.Builder(this).setTitle(r.loadLabel(pm))
      .setItems(new String[]{"Quitar del inicio"}, (d, w) -> { List<String> ps = pins(); ps.remove(p); savePins(ps); }).show(); return true; });
    return l;
  }

  android.graphics.drawable.GradientDrawable glow() {
    android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
    g.setCornerRadius(dp(18)); g.setColor(0x332A0A4A); g.setStroke(dp(1), 0x88A855F7); return g;
  }

  Button btn(String s, View.OnClickListener c) {
    Button b = new Button(this); b.setText(s); b.setTextColor(0xFFF3E8FF); b.setAllCaps(false);
    android.graphics.drawable.GradientDrawable g = glow(); g.setCornerRadius(dp(24)); b.setBackground(g);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(130), dp(46)); lp.setMargins(dp(8),0,dp(8),0);
    b.setLayoutParams(lp); b.setOnClickListener(c); return b;
  }

  void showDrawer(boolean s) { root.findViewWithTag("d").setVisibility(s ? View.VISIBLE : View.GONE); }
  @Override public void onBackPressed() { showDrawer(false); }
  void launch(String p) { Intent i = pm.getLaunchIntentForPackage(p); if (i != null) startActivity(i); showDrawer(false); }
  void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
  int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }

  /** Fondos void morados dibujados por código */
  static class VoidBg extends View {
    static final int N = 5; int w; Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); Random rnd = new Random();
    static final int[][] C = { {0xFF2E0854,0xFF0A0014}, {0xFF4C1D95,0xFF05000A}, {0xFF6B21A8,0xFF12001F},
      {0xFF1E0B3A,0xFF000000}, {0xFF7E22CE,0xFF0B0016} };
    VoidBg(Context c, int w) { super(c); this.w = w % N; }
    void set(int i) { w = i; invalidate(); }
    @Override protected void onDraw(Canvas c) {
      int W = getWidth(), H = getHeight(); float cy = H * (w % 2 == 0 ? .35f : .6f);
      p.setShader(new RadialGradient(W/2f, cy, Math.max(W,H)*.8f, C[w][0], C[w][1], Shader.TileMode.CLAMP));
      c.drawRect(0,0,W,H,p); p.setShader(null);
      rnd.setSeed(w * 99L); p.setColor(0xFFE9D5FF);
      for (int i=0;i<120;i++){ p.setAlpha(40+rnd.nextInt(180)); c.drawCircle(rnd.nextInt(W), rnd.nextInt(H), rnd.nextFloat()*2.5f, p); }
      p.setAlpha(255); p.setStyle(Paint.Style.STROKE);
      if (w == 1 || w == 4) { // anillos del vacío
        for (int i=1;i<=4;i++){ p.setStrokeWidth(3); p.setColor(Color.argb(90/i,192,132,252)); c.drawCircle(W/2f, cy, W*.14f*i, p); }
        p.setStyle(Paint.Style.FILL); p.setColor(0xFF000000); c.drawCircle(W/2f, cy, W*.12f, p);
      } else if (w == 2) { // grieta
        p.setStrokeWidth(4); p.setColor(0xAAD8B4FE); Path pa = new Path(); pa.moveTo(W*.5f,0);
        for (int y=0;y<=H;y+=H/12) pa.lineTo(W*.5f + (rnd.nextFloat()-.5f)*W*.25f, y); c.drawPath(pa,p);
      }
      p.setStyle(Paint.Style.FILL);
    }
  }
}
