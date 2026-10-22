package com.tricheer.guard;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 桌面格子图标修正：launcher 不认我们 Activity 的 android:icon，
 * 所以主界面显示出来后，按标签找到对应格子并把图标换掉。
 */
public final class TileIcons implements Application.ActivityLifecycleCallbacks {

    private static final String MAIN = "com.tricheer.launcherk218.ActivityMain";
    private static final String[][] MAP = {
            {"全部应用", "ic_guard_apps"},
            {"守护设置", "ic_guard_settings"},
            {"打开DiPlay", "ic_guard_diplay"},
    };

    private static volatile boolean sInstalled = false;

    private TileIcons() {
    }

    public static void install(Application app) {
        if (sInstalled) {
            return;
        }
        sInstalled = true;
        app.registerActivityLifecycleCallbacks(new TileIcons());
    }

    @Override
    public void onActivityResumed(final Activity a) {
        if (!MAIN.equals(a.getClass().getName())) {
            return;
        }
        Handler h = new Handler(Looper.getMainLooper());
        for (int delay : new int[]{600, 1500, 3000}) {
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        patch(a);
                    } catch (Throwable ignored) {
                    }
                }
            }, delay);
        }
    }

    private void patch(Activity a) {
        View root = a.getWindow().getDecorView();
        int changed = 0;
        for (String[] e : MAP) {
            View tv = findByText(root, e[0]);
            if (tv == null) {
                continue;
            }
            ImageView iv = findImageNear(tv);
            if (iv == null) {
                continue;
            }
            int id = a.getResources().getIdentifier(e[1], "drawable", a.getPackageName());
            if (id == 0) {
                continue;
            }
            Drawable d = a.getResources().getDrawable(id);
            if (d == null) {
                continue;
            }
            if (sameDrawable(iv.getDrawable(), d)) {
                continue;               // 已经是我们的图标，不重复设置
            }
            iv.setImageDrawable(d);
            changed++;
        }
        changed += patchPinned(a, root);
        if (changed > 0) {
            android.util.Log.i("K218Guard", "tile icons patched: " + changed);
        }
    }

    /** 常驻到桌面的应用：用它们自己的图标 */
    private int patchPinned(Activity a, View root) {
        int n = 0;
        try {
            SharedPreferences sp = a.getSharedPreferences("guard_ui", Context.MODE_PRIVATE);
            String raw = sp.getString("pinned", "");
            if (raw == null || raw.length() == 0) {
                return 0;
            }
            PackageManager pm = a.getPackageManager();
            for (String item : raw.split("\n")) {
                int slash = item.indexOf('/');
                if (slash <= 0) {
                    continue;
                }
                String pkg = item.substring(0, slash);
                String cls = item.substring(slash + 1);
                try {
                    ActivityInfo ai = pm.getActivityInfo(new ComponentName(pkg, cls), 0);
                    CharSequence label = ai.loadLabel(pm);
                    Drawable icon = ai.loadIcon(pm);
                    if (label == null || icon == null) {
                        continue;
                    }
                    View tv = findByText(root, label.toString());
                    if (tv == null) {
                        continue;
                    }
                    ImageView iv = findImageNear(tv);
                    if (iv == null || sameDrawable(iv.getDrawable(), icon)) {
                        continue;
                    }
                    iv.setImageDrawable(icon);
                    n++;
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return n;
    }

    private boolean sameDrawable(Drawable cur, Drawable want) {
        if (cur == null) {
            return false;
        }
        android.graphics.drawable.Drawable.ConstantState a = cur.getConstantState();
        android.graphics.drawable.Drawable.ConstantState b = want.getConstantState();
        return a != null && a.equals(b);
    }

    private View findByText(View v, String text) {
        if (v instanceof TextView) {
            CharSequence t = ((TextView) v).getText();
            if (t != null && text.contentEquals(t)) {
                return v;
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View r = findByText(g.getChildAt(i), text);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    private ImageView findImageNear(View textView) {
        View p = textView;
        for (int depth = 0; depth < 4 && p != null; depth++) {
            if (p instanceof ViewGroup) {
                ImageView iv = findImageView((ViewGroup) p);
                if (iv != null) {
                    return iv;
                }
            }
            android.view.ViewParent parent = p.getParent();
            p = parent instanceof View ? (View) parent : null;
        }
        return null;
    }

    private ImageView findImageView(ViewGroup g) {
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            if (c instanceof ImageView) {
                return (ImageView) c;
            }
            if (c instanceof ViewGroup) {
                ImageView r = findImageView((ViewGroup) c);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    @Override
    public void onActivityCreated(Activity a, Bundle b) {
    }

    @Override
    public void onActivityStarted(Activity a) {
    }

    @Override
    public void onActivityPaused(Activity a) {
    }

    @Override
    public void onActivityStopped(Activity a) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity a, Bundle b) {
    }

    @Override
    public void onActivityDestroyed(Activity a) {
    }
}
