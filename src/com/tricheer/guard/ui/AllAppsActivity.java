package com.tricheer.guard.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.UnderlineSpan;
import android.widget.Toast;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 全部应用 + 顶部常驻区（纯代码构建 UI）
 *  - 顶部常驻区：可添加/移除，长按菜单（移除/卸载/查看详情）
 *  - 下方全部应用：长按无菜单（只启动）
 *  - 整页纵向滚动，常驻区随页面一起滚动
 */
public class AllAppsActivity extends Activity {

    private static final int BG = 0xFF101418;
    private static final int FG = 0xFFEDEFF2;
    private static final int SUB = 0xFF8A929B;
    private static final int CARD = 0xFF1B2027;
    private static final int ACCENT = 0xFF6FA8FF;

    private static final String SP = "guard_ui";
    private static final String KEY_PINNED = "pinned";
    private static final String KEY_OWNED = "owned";   // 我们往桌面加过的条目：cls=次数
    private static final String KEY_BASE = "base";     // 添加前的行数（现场记录）

    private final List<ResolveInfo> mAll = new ArrayList<ResolveInfo>();
    private final List<String> mPinned = new ArrayList<String>();

    private LinearLayout mPinnedRow;
    private LinearLayout mGridBox;
    private ScrollView mScroll;
    private SharedPreferences mSp;
    private boolean mFirstResume = true;
    private boolean mRestartArmed = false;

    private final BroadcastReceiver mPkgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            refreshIfChanged();
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        mSp = getSharedPreferences(SP, MODE_PRIVATE);
        loadPinned();
        setContentView(buildRoot());
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            IntentFilter f = new IntentFilter();
            f.addAction(Intent.ACTION_PACKAGE_ADDED);
            f.addAction(Intent.ACTION_PACKAGE_REMOVED);
            f.addAction(Intent.ACTION_PACKAGE_REPLACED);
            f.addAction(Intent.ACTION_PACKAGE_CHANGED);
            f.addDataScheme("package");
            registerReceiver(mPkgReceiver, f);
        } catch (Throwable ignored) {
        }
        refreshIfChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(mPkgReceiver);
        } catch (Throwable ignored) {
        }
    }

    /** 只有应用列表真的变了才重建界面，避免无谓的跳动 */
    private void refreshIfChanged() {
        String before = signature(mAll);
        reloadApps();
        if (mFirstResume || !before.equals(signature(mAll))) {
            mFirstResume = false;
            refresh();
        }
    }

    private String signature(List<ResolveInfo> list) {
        StringBuilder sb = new StringBuilder();
        for (ResolveInfo ri : list) {
            sb.append(keyOf(ri)).append(';');
        }
        return sb.toString();
    }

    // ---------- data ----------

    private void loadPinned() {
        mPinned.clear();
        String raw = mSp.getString(KEY_PINNED, "");
        if (raw != null && raw.length() > 0) {
            for (String s : raw.split("\n")) {
                if (s.length() > 0) {
                    mPinned.add(s);
                }
            }
        }
    }

    private void savePinned() {
        StringBuilder sb = new StringBuilder();
        for (String s : mPinned) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(s);
        }
        mSp.edit().putString(KEY_PINNED, sb.toString()).apply();
    }

    private void reloadApps() {
        mAll.clear();
        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(i, 0);
        if (list != null) {
            mAll.addAll(list);
        }
        Collections.sort(mAll, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo a, ResolveInfo c) {
                return label(a).toString().compareToIgnoreCase(label(c).toString());
            }
        });
    }

    private CharSequence label(ResolveInfo ri) {
        try {
            return ri.loadLabel(getPackageManager());
        } catch (Throwable e) {
            return ri.activityInfo.packageName;
        }
    }

    private ResolveInfo find(String key) {
        for (ResolveInfo ri : mAll) {
            if (keyOf(ri).equals(key)) {
                return ri;
            }
        }
        return null;
    }

    private static String keyOf(ResolveInfo ri) {
        return ri.activityInfo.packageName + "/" + ri.activityInfo.name;
    }

    // ---------- ui ----------

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

    private View buildRoot() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);
        mScroll = sv;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(32), dp(24), dp(32), dp(32));
        sv.addView(root);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("全部应用");
        title.setTextColor(FG);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        titleRow.addView(backButton());
        titleRow.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(titleRow);

        TextView sub = new TextView(this);
        SpannableString ss = new SpannableString(
                "常驻在桌面的应用，长按图标可移除 / 卸载 / 查看详情\n"
                        + "其它应用长按可添加到常驻，添加后会同步显示到桌面\n"
                        + "桌面图标重启后刷新。立即重启");
        int hi = ss.toString().indexOf("立即重启");
        if (hi >= 0) {
            ss.setSpan(new UnderlineSpan(), hi, hi + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        sub.setText(ss);
        sub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRestartClicked();
            }
        });
        sub.setTextColor(SUB);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        sub.setPadding(0, dp(6), 0, dp(18));
        root.addView(sub);

        // 常驻区（横向可滚动，纵向跟随页面）
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        mPinnedRow = new LinearLayout(this);
        mPinnedRow.setOrientation(LinearLayout.HORIZONTAL);
        mPinnedRow.setPadding(0, 0, 0, dp(8));
        hsv.addView(mPinnedRow);
        root.addView(hsv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View line = new View(this);
        line.setBackgroundColor(0xFF262C34);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.topMargin = dp(8);
        lp.bottomMargin = dp(20);
        root.addView(line, lp);

        mGridBox = new LinearLayout(this);
        mGridBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(mGridBox);

        root.addView(Footer.build(this));
        return sv;
    }

    private void refresh() {
        final int y = mScroll == null ? 0 : mScroll.getScrollY();
        rebuildPinned();
        rebuildGrid();
        if (mScroll != null) {
            mScroll.post(new Runnable() {
                @Override
                public void run() {
                    mScroll.scrollTo(0, y);   // 重建后恢复原来的滚动位置
                }
            });
        }
    }

    private void rebuildPinned() {
        mPinnedRow.removeAllViews();
        boolean pruned = false;
        for (int i = 0; i < mPinned.size(); ) {
            final ResolveInfo ri = find(mPinned.get(i));
            if (ri == null) {
                if (mAll.isEmpty()) {
                    i++;                    // 应用列表还没加载，先保留，别误删
                    continue;
                }
                String deadCls = mPinned.get(i);
                int slash = deadCls.indexOf('/');
                if (slash > 0) {
                    deadCls = deadCls.substring(slash + 1);
                }
                mPinned.remove(i);          // 应用确实已不存在，剔除僵尸条目
                pruned = true;
                // 顺带把桌面上我们加的那行也删掉，避免留下死图标
                if (ownedCount(deadCls) > 0
                        && com.tricheer.guard.Desktop.removeOne(this, deadCls)) {
                    int left = ownedCount(deadCls) - 1;
                    setOwnedCount(deadCls, left);
                    if (left == 0) {
                        setBaseCount(deadCls, -1);
                    }
                }
                continue;
            }
            View cell = makeCell(ri.loadIcon(getPackageManager()), label(ri), true, ri);
            cell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launch(ri);
                }
            });
            cell.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showPinMenu(ri);
                    return true;
                }
            });
            mPinnedRow.addView(cell, cellParams());
            i++;
        }
        if (pruned) {
            savePinned();
        }
        mPinnedRow.addView(makeAddCell(), cellParams());
    }

    private void rebuildGrid() {
        mGridBox.removeAllViews();
        int cols = Math.max(3, getResources().getDisplayMetrics().widthPixels / dp(190));
        LinearLayout row = null;
        for (int i = 0; i < mAll.size(); i++) {
            if (i % cols == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 0, 0, dp(18));
                mGridBox.addView(row);
            }
            final ResolveInfo ri = mAll.get(i);
            View cell = makeCell(ri.loadIcon(getPackageManager()), label(ri), false, ri);
            cell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launch(ri);
                }
            });
            cell.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showAppMenu(ri);
                    return true;
                }
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(cell, p);
        }
        // 补齐最后一行
        if (row != null) {
            int filled = mAll.size() % cols;
            if (filled != 0) {
                for (int i = filled; i < cols; i++) {
                    View blank = new View(this);
                    row.addView(blank, new LinearLayout.LayoutParams(0,
                            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                }
            }
        }
    }

    private LinearLayout.LayoutParams cellParams() {
        return new LinearLayout.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private View makeCell(Drawable icon, CharSequence text, boolean pinned, ResolveInfo ri) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(8), dp(8), dp(8), dp(8));

        ImageView iv = new ImageView(this);
        iv.setImageDrawable(icon);
        box.addView(iv, new LinearLayout.LayoutParams(dp(52), dp(52)));

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(FG);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setGravity(Gravity.CENTER);
        tv.setMaxLines(1);
        tv.setPadding(0, dp(6), 0, 0);
        box.addView(tv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return box;
    }

    private View makeAddCell() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(8), dp(8), dp(8), dp(8));

        TextView plus = new TextView(this);
        plus.setText("+");
        plus.setTextColor(ACCENT);
        plus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
        plus.setGravity(Gravity.CENTER);
        plus.setBackgroundColor(CARD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(52), dp(52));
        box.addView(plus, p);

        TextView tv = new TextView(this);
        tv.setText("添加");
        tv.setTextColor(SUB);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(6), 0, 0);
        box.addView(tv);

        box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPicker();
            }
        });
        return box;
    }

    // ---------- actions ----------

    private void launch(ResolveInfo ri) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Throwable ignored) {
        }
    }

    private void showPinMenu(final ResolveInfo ri) {
        final String[] items = {"从常驻移除", "卸载", "查看详情"};
        new AlertDialog.Builder(this)
                .setTitle(label(ri))
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0) {
                            doUnpin(ri);
                        } else if (which == 1) {
                            try {
                                startActivity(new Intent(Intent.ACTION_DELETE,
                                        Uri.parse("package:" + ri.activityInfo.packageName)));
                            } catch (Throwable ignored) {
                            }
                        } else {
                            try {
                                startActivity(new Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:" + ri.activityInfo.packageName)));
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showAppMenu(final ResolveInfo ri) {
        final String[] items = {"添加到常驻", "卸载", "查看详情"};
        new AlertDialog.Builder(this)
                .setTitle(label(ri))
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0) {
                            doPin(ri);
                        } else if (which == 1) {
                            uninstall(ri);
                        } else {
                            details(ri);
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 加入常驻 + 同步到桌面 */
    private void doPin(ResolveInfo ri) {
        String k = keyOf(ri);
        if (mPinned.contains(k)) {
            try {
                Toast.makeText(this, "已在常驻中", Toast.LENGTH_SHORT).show();
            } catch (Throwable ignored) {
            }
            return;                     // 已在常驻，不再重复插入桌面行
        }
        mPinned.add(k);
        savePinned();
        rebuildPinned();
        String cls = ri.activityInfo.name;
        if (baseCount(cls) < 0) {
            int before = com.tricheer.guard.Desktop.rowCount(this, cls);
            if (before >= 0) {
                setBaseCount(cls, before);   // 现场记录"加之前有几行"
            }
        }
        long id = com.tricheer.guard.Desktop.add(this, cls);   // 重复也加
        if (id > 0) {
            setOwnedCount(cls, ownedCount(cls) + 1);
        }
    }

    /** 从常驻移除；桌面行只删我们自己加过的，原有图标一律不动 */
    private void doUnpin(ResolveInfo ri) {
        mPinned.remove(keyOf(ri));
        savePinned();
        rebuildPinned();
        String cls = ri.activityInfo.name;
        if (ownedCount(cls) > 0) {
            // 安全阀：只有"行数 > 基线"才删，确保不会删掉自带的图标
            int base = baseCount(cls);
            int actual = com.tricheer.guard.Desktop.rowCount(this, cls);
            if (base < 0) {
                base = 0;
            }
            if (actual > base && com.tricheer.guard.Desktop.removeOne(this, cls)) {
                int left = ownedCount(cls) - 1;
                setOwnedCount(cls, left);
                if (left == 0) {
                    setBaseCount(cls, -1);   // 没我们的行了，基线也清掉
                }
            }
        }
    }

    private int baseCount(String cls) {
        String raw = mSp.getString(KEY_BASE, "");
        if (raw != null && raw.length() > 0) {
            for (String line : raw.split("\n")) {
                int i = line.lastIndexOf('=');
                if (i > 0 && line.substring(0, i).equals(cls)) {
                    try {
                        return Integer.parseInt(line.substring(i + 1));
                    } catch (Throwable ignored) {
                        return -1;
                    }
                }
            }
        }
        return -1;
    }

    private void setBaseCount(String cls, int n) {
        java.util.LinkedHashMap<String, Integer> map = new java.util.LinkedHashMap<String, Integer>();
        String raw = mSp.getString(KEY_BASE, "");
        if (raw != null && raw.length() > 0) {
            for (String line : raw.split("\n")) {
                int i = line.lastIndexOf('=');
                if (i > 0) {
                    try {
                        map.put(line.substring(0, i), Integer.parseInt(line.substring(i + 1)));
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        if (n < 0) {
            map.remove(cls);
        } else {
            map.put(cls, n);
        }
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, Integer> e : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        mSp.edit().putString(KEY_BASE, sb.toString()).apply();
    }

    private int ownedCount(String cls) {
        String raw = mSp.getString(KEY_OWNED, "");
        if (raw != null && raw.length() > 0) {
            for (String line : raw.split("\n")) {
                int i = line.lastIndexOf('=');
                if (i > 0 && line.substring(0, i).equals(cls)) {
                    try {
                        return Integer.parseInt(line.substring(i + 1));
                    } catch (Throwable ignored) {
                        return 0;
                    }
                }
            }
        }
        return 0;
    }

    private void setOwnedCount(String cls, int n) {
        java.util.LinkedHashMap<String, Integer> map = new java.util.LinkedHashMap<String, Integer>();
        String raw = mSp.getString(KEY_OWNED, "");
        if (raw != null && raw.length() > 0) {
            for (String line : raw.split("\n")) {
                int i = line.lastIndexOf('=');
                if (i > 0) {
                    try {
                        map.put(line.substring(0, i), Integer.parseInt(line.substring(i + 1)));
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        if (n <= 0) {
            map.remove(cls);
        } else {
            map.put(cls, n);
        }
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, Integer> e : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        mSp.edit().putString(KEY_OWNED, sb.toString()).apply();
    }

    /** 点两次才重启桌面 */
    private void onRestartClicked() {
        if (!mRestartArmed) {
            mRestartArmed = true;
            try {
                Toast.makeText(this, "再点一次重启桌面", Toast.LENGTH_SHORT).show();
            } catch (Throwable ignored) {
            }
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    mRestartArmed = false;
                }
            }, 5000);
            return;
        }
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void uninstall(ResolveInfo ri) {
        try {
            startActivity(new Intent(Intent.ACTION_DELETE,
                    Uri.parse("package:" + ri.activityInfo.packageName)));
        } catch (Throwable ignored) {
        }
    }

    private void details(ResolveInfo ri) {
        try {
            startActivity(new Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + ri.activityInfo.packageName)));
        } catch (Throwable ignored) {
        }
    }

    private void showPicker() {
        final List<ResolveInfo> candidates = new ArrayList<ResolveInfo>();
        final List<String> names = new ArrayList<String>();
        for (ResolveInfo ri : mAll) {
            String k = keyOf(ri);
            if (mPinned.contains(k)) {
                continue;
            }
            candidates.add(ri);
            names.add(label(ri).toString());
        }
        if (candidates.isEmpty()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("添加到常驻")
                .setItems(names.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        mPinned.add(keyOf(candidates.get(which)));
                        savePinned();
                        rebuildPinned();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
