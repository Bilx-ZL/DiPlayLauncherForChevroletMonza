package com.tricheer.guard.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.tricheer.guard.Cfg;

/** 守护设置界面（纯代码 UI） */
public class SettingsActivity extends Activity {

    private static final int BG = 0xFF101418;
    private static final int FG = 0xFFEDEFF2;
    private static final int SUB = 0xFF8A929B;
    private static final int ACCENT = 0xFF6FA8FF;
    private static final int LINE = 0xFF262C34;
    private static final int BTN = 0xFF232A33;

    private SharedPreferences mSp;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        mSp = Cfg.sp(this);
        setContentView(build());
    }

    private View backButton() {
        TextView b = new TextView(this);
        b.setText("← 返回");
        b.setTextColor(FG);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        b.setPadding(0, dp(8), dp(28), dp(8));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        return b;
    }

    private View build() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(32), dp(24), dp(32), dp(32));
        sv.addView(root);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("守护设置");
        title.setTextColor(FG);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        titleRow.addView(backButton());
        titleRow.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(titleRow);

        root.addView(section("守护开关"));
        root.addView(switchRow("WiFi 热点守护", "保持 wifi_ap_state = on", Cfg.K_AP_ON, Cfg.DEF_AP_ON));
        root.addView(divider());
        root.addView(switchRow("DiPlay 守护", "进程消失后自动拉起", Cfg.K_DP_ON, Cfg.DEF_DP_ON));

        root.addView(section("逻辑设置"));
        root.addView(valueRow("WiFi 轮询间隔", "多久检查一次 wifi_ap_state",
                Cfg.K_AP_IVL, Cfg.DEF_AP_IVL, new int[]{5, 10, 15, 30, 60}));
        root.addView(divider());
        root.addView(valueRow("DiPlay 轮询间隔", "多久检查一次 DiPlay 进程",
                Cfg.K_DP_IVL, Cfg.DEF_DP_IVL, new int[]{5, 10, 15, 30, 60}));
        root.addView(divider());
        root.addView(valueRow("DiPlay 拉起延迟", "发现进程消失后等待多久再拉起",
                Cfg.K_DP_DELAY, Cfg.DEF_DP_DELAY, new int[]{0, 3, 5, 10, 15, 30}));

        root.addView(restoreButton());
        root.addView(Footer.build(this));
        return sv;
    }

    private View restoreButton() {
        TextView btn = new TextView(this);
        btn.setText("恢复默认设置");
        btn.setTextColor(FG);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        btn.setGravity(Gravity.CENTER);
        btn.setBackgroundColor(BTN);
        btn.setPadding(0, dp(16), 0, dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(32);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmRestore();
            }
        });
        return btn;
    }

    private void confirmRestore() {
        new AlertDialog.Builder(this)
                .setTitle("恢复默认设置？")
                .setMessage("两个守护会恢复为关闭，\n轮询间隔恢复为 5 秒，拉起延迟恢复为 3 秒。")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        mSp.edit().clear().apply();
                        recreate();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private View section(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(ACCENT);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t.setPadding(0, dp(24), 0, dp(8));
        return t;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        return v;
    }

    private View switchRow(String title, String sub, final String key, boolean def) {
        LinearLayout row = rowShell();
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(textMain(title));
        left.addView(textSub(sub));
        row.addView(left, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch sw = new Switch(this);
        sw.setChecked(mSp.getBoolean(key, def));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                mSp.edit().putBoolean(key, checked).apply();
            }
        });
        row.addView(sw);
        return row;
    }

    private View valueRow(String title, String sub, final String key, int def, final int[] options) {
        LinearLayout row = rowShell();
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(textMain(title));
        left.addView(textSub(sub));
        row.addView(left, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView val = new TextView(this);
        val.setText(mSp.getInt(key, def) + " 秒");
        val.setTextColor(ACCENT);
        val.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        val.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(val);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] items = new String[options.length];
                for (int i = 0; i < options.length; i++) {
                    items[i] = options[i] + " 秒";
                }
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("选择间隔")
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                mSp.edit().putInt(key, options[which]).apply();
                                val.setText(options[which] + " 秒");
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
        return row;
    }

    private LinearLayout rowShell() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(16), 0, dp(16));
        row.setBackgroundColor(Color.TRANSPARENT);
        return row;
    }

    private TextView textMain(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(FG);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        return t;
    }

    private TextView textSub(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(SUB);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t.setPadding(0, dp(4), 0, 0);
        return t;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
