package com.tricheer.guard;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;

/** 确保桌面数据库里有我们的两个入口（刷机/重装后自动补） */
public final class DbSeed {

    private static final String TAG = "K218Guard";
    private static final String DB = "Database.db";
    private static final String[] ENTRIES = {
            "com.tricheer.guard.ui.AllAppsActivity",
            "com.tricheer.guard.ui.SettingsActivity",
            "com.tricheer.guard.ui.OpenDiPlay",
    };

    private DbSeed() {
    }

    /** 额外把 SharedPreferences 里已常驻的应用补进桌面 */
    private static void seedPinned(Context ctx, SQLiteDatabase db) {
        try {
            android.content.SharedPreferences sp =
                    ctx.getSharedPreferences("guard_ui", Context.MODE_PRIVATE);
            String raw = sp.getString("pinned", "");
            if (raw == null || raw.length() == 0) {
                return;
            }
            for (String item : raw.split("\n")) {
                int slash = item.indexOf('/');
                if (slash <= 0 || slash + 1 >= item.length()) {
                    continue;
                }
                String cls = item.substring(slash + 1);
                Cursor c = db.rawQuery("SELECT _id FROM apps_data WHERE class_name=?", new String[]{cls});
                boolean exists = c != null && c.moveToFirst();
                if (c != null) {
                    c.close();
                }
                if (!exists) {
                    db.execSQL("INSERT INTO apps_data(class_name) VALUES(?)", new Object[]{cls});
                    markOwned(ctx, cls);
                    Log.i(TAG, "db seed pinned: " + cls);
                }
            }
        } catch (Throwable e) {
            Log.w(TAG, "seed pinned failed: " + e);
        }
    }

    /** 记录"这一行是我们加的"，移除常驻时只删自己的 */
    private static void markOwned(Context ctx, String cls) {
        try {
            android.content.SharedPreferences sp =
                    ctx.getSharedPreferences("guard_ui", Context.MODE_PRIVATE);
            String raw = sp.getString("owned", "");
            java.util.LinkedHashMap<String, Integer> map =
                    new java.util.LinkedHashMap<String, Integer>();
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
            Integer cur = map.get(cls);
            map.put(cls, cur == null ? 1 : cur + 1);
            StringBuilder sb = new StringBuilder();
            for (java.util.Map.Entry<String, Integer> e : map.entrySet()) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(e.getKey()).append('=').append(e.getValue());
            }
            sp.edit().putString("owned", sb.toString()).apply();
        } catch (Throwable ignored) {
        }
    }


    /** 启动时清掉"解析不到已安装应用、且不是厂家自带"的行 */
    private static void pruneDead(Context ctx, SQLiteDatabase db) {
        try {
            java.util.HashSet<String> alive = new java.util.HashSet<String>();
            android.content.Intent probe = new android.content.Intent(android.content.Intent.ACTION_MAIN);
            probe.addCategory(android.content.Intent.CATEGORY_LAUNCHER);
            java.util.List<android.content.pm.ResolveInfo> list =
                    ctx.getPackageManager().queryIntentActivities(probe, 0);
            if (list != null) {
                for (android.content.pm.ResolveInfo ri : list) {
                    alive.add(ri.activityInfo.name);
                }
            }
            java.util.ArrayList<String> dead = new java.util.ArrayList<String>();
            Cursor c = db.rawQuery("SELECT class_name FROM apps_data", null);
            if (c != null) {
                while (c.moveToNext()) {
                    String cls = c.getString(0);
                    if (cls != null && !alive.contains(cls) && !isBaseline(cls)) {
                        dead.add(cls);
                    }
                }
                c.close();
            }
            for (String cls : dead) {
                db.execSQL("DELETE FROM apps_data WHERE class_name=?", new Object[]{cls});
                Log.i(TAG, "prune dead row: " + cls);
            }
        } catch (Throwable e) {
            Log.w(TAG, "prune failed: " + e);
        }
    }

    /** launcher 自带的固定条目（每个应恰好 1 行） */
    private static final String[] BASELINE = {
            "com.tricheer.radio.MainActivity",
            "com.yj.audio.WelcomeActivity",
            "com.tricheer.bt.MainActivity",
            "com.cruisecloud.dvr.SplashActivity",
            "com.autonavi.auto.remote.fill.UsbFillActivity",
            "com.tricheer.carlife.CarLifeActivity",
            "com.sgm.cadillac.function.launch.activity.LoadingActivity",
            "com.tricheer.settings.MainActivity",
            "com.sgm.onstarcarlink.function.launch.activity.LaunchActivity",
            "com.tricheer.bt.MusicActivity",
            "com.tricheer.guard.ui.OpenDiPlay",
            "com.tricheer.guard.ui.AllAppsActivity",
            "com.tricheer.guard.ui.SettingsActivity",
    };

    private static boolean isBaseline(String cls) {
        for (String b : BASELINE) {
            if (b.equals(cls)) {
                return true;
            }
        }
        return false;
    }

    private static int countRows(SQLiteDatabase db, String cls) {
        Cursor c = db.rawQuery("SELECT count(*) FROM apps_data WHERE class_name=?",
                new String[]{cls});
        int n = 0;
        if (c != null) {
            if (c.moveToFirst()) {
                n = c.getInt(0);
            }
            c.close();
        }
        return n;
    }

    /** 用数据库真实行数校正 owned 计数，并清掉多出来的行 */
    private static void reconcile(Context ctx, SQLiteDatabase db) {
        try {
            android.content.SharedPreferences sp =
                    ctx.getSharedPreferences("guard_ui", Context.MODE_PRIVATE);
            String raw = sp.getString("owned", "");
            java.util.LinkedHashMap<String, Integer> map =
                    new java.util.LinkedHashMap<String, Integer>();
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
            java.util.LinkedHashMap<String, Integer> bases =
                    new java.util.LinkedHashMap<String, Integer>();
            String rawBase = sp.getString("base", "");
            if (rawBase != null && rawBase.length() > 0) {
                for (String line : rawBase.split("\n")) {
                    int i = line.lastIndexOf('=');
                    if (i > 0) {
                        try {
                            bases.put(line.substring(0, i),
                                    Integer.parseInt(line.substring(i + 1)));
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
            for (String cls : new java.util.ArrayList<String>(map.keySet())) {
                int recorded = map.get(cls);
                int actual = countRows(db, cls);
                Integer savedBase = bases.get(cls);
                int base = savedBase != null
                        ? savedBase
                        : (isBaseline(cls) ? 1 : 0);   // 老数据退回硬编码基线
                int mine = Math.max(0, actual - base);
                if (mine != recorded) {
                    Log.i(TAG, "reconcile " + cls + ": " + recorded + " -> " + mine);
                    if (mine == 0) {
                        map.remove(cls);
                        bases.remove(cls);      // 没有我们的行了，基线也别留
                    } else {
                        map.put(cls, mine);
                    }
                }
                int extra = actual - base - mine;
                for (int i = 0; i < extra; i++) {
                    db.execSQL("DELETE FROM apps_data WHERE _id IN "
                            + "(SELECT _id FROM apps_data WHERE class_name=? LIMIT 1)",
                            new Object[]{cls});
                    Log.i(TAG, "reconcile drop extra row: " + cls);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (java.util.Map.Entry<String, Integer> e : map.entrySet()) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(e.getKey()).append('=').append(e.getValue());
            }
            sp.edit().putString("owned", sb.toString()).apply();
            for (String bcls : new java.util.ArrayList<String>(bases.keySet())) {
                if (!map.containsKey(bcls)) {
                    bases.remove(bcls);          // 孤立基线残留，清掉
                }
            }
            StringBuilder bs = new StringBuilder();
            for (java.util.Map.Entry<String, Integer> e : bases.entrySet()) {
                if (bs.length() > 0) {
                    bs.append('\n');
                }
                bs.append(e.getKey()).append('=').append(e.getValue());
            }
            sp.edit().putString("base", bs.toString()).apply();
        } catch (Throwable e) {
            Log.w(TAG, "reconcile failed: " + e);
        }
    }

    /** @return true 表示数据库已就绪（不管是否新插入） */
    public static boolean ensure(Context ctx) {
        File f = ctx.getDatabasePath(DB);
        if (!f.exists()) {
            return false;
        }
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
            for (String cls : ENTRIES) {
                Cursor c = db.rawQuery("SELECT _id FROM apps_data WHERE class_name=?", new String[]{cls});
                boolean exists = c != null && c.moveToFirst();
                if (c != null) {
                    c.close();
                }
                if (!exists) {
                    db.execSQL("INSERT INTO apps_data(class_name) VALUES(?)", new Object[]{cls});
                    Log.i(TAG, "db seed: added " + cls);
                }
            }
            seedPinned(ctx, db);
            reconcile(ctx, db);
            pruneDead(ctx, db);
            return true;
        } catch (Throwable e) {
            Log.w(TAG, "db seed failed: " + e);
            return false;
        } finally {
            if (db != null) {
                try {
                    db.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
