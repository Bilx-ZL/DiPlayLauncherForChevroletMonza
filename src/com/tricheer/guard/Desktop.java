package com.tricheer.guard;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;

/**
 * 车机桌面图标的增删（写 launcher 自己的 Database.db → apps_data 表）
 * 只操作 class_name 一列，绝不动其它字段。
 */
public final class Desktop {

    private static final String TAG = "K218Guard";
    private static final String DB = "Database.db";

    /** 这几个入口永远保留，不允许被删 */
    private static final String[] PROTECTED = {
            "com.tricheer.guard.ui.OpenDiPlay",
            "com.tricheer.guard.ui.AllAppsActivity",
            "com.tricheer.guard.ui.SettingsActivity",
    };

    private Desktop() {
    }

    /** 总是新增一行（重复也加），返回新行 id，失败返回 -1 */
    public static long add(Context ctx, String className) {
        if (className == null || className.length() == 0) {
            return -1L;
        }
        return insert(ctx, className);
    }

    /** 删除该 class 的任意一行（我们加的那条），成功返回 true */
    public static boolean removeOne(Context ctx, String className) {
        if (className == null || className.length() == 0 || isProtected(className)) {
            return false;
        }
        return deleteOne(ctx, className);
    }

    private static long insert(Context ctx, String cls) {
        File f = ctx.getDatabasePath(DB);
        if (!f.exists()) {
            return -1L;
        }
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
            db.execSQL("INSERT INTO apps_data(class_name) VALUES(?)", new Object[]{cls});
            Cursor c = db.rawQuery("SELECT last_insert_rowid()", null);
            long id = -1L;
            if (c != null && c.moveToFirst()) {
                id = c.getLong(0);
                c.close();
            }
            Log.i(TAG, "desktop add: " + cls + " id=" + id);
            return id;
        } catch (Throwable e) {
            Log.w(TAG, "desktop add failed: " + e);
            return -1L;
        } finally {
            if (db != null) {
                try {
                    db.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static boolean deleteOne(Context ctx, String cls) {
        File f = ctx.getDatabasePath(DB);
        if (!f.exists()) {
            return false;
        }
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
            db.execSQL("DELETE FROM apps_data WHERE _id IN "
                    + "(SELECT _id FROM apps_data WHERE class_name=? LIMIT 1)", new Object[]{cls});
            Log.i(TAG, "desktop remove one: " + cls);
            return true;
        } catch (Throwable e) {
            Log.w(TAG, "desktop remove failed: " + e);
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

    @Deprecated
    private static boolean edit(Context ctx, String cls, boolean add) {
        File f = ctx.getDatabasePath(DB);
        if (!f.exists()) {
            return false;
        }
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
            Cursor c = db.rawQuery("SELECT _id FROM apps_data WHERE class_name=?", new String[]{cls});
            boolean exists = c != null && c.moveToFirst();
            if (c != null) {
                c.close();
            }
            if (add && !exists) {
                db.execSQL("INSERT INTO apps_data(class_name) VALUES(?)", new Object[]{cls});
                Log.i(TAG, "desktop add: " + cls);
                return true;
            }
            if (!add && exists) {
                db.execSQL("DELETE FROM apps_data WHERE class_name=?", new Object[]{cls});
                Log.i(TAG, "desktop remove: " + cls);
                return true;
            }
        } catch (Throwable e) {
            Log.w(TAG, "desktop edit failed: " + e);
        } finally {
            if (db != null) {
                try {
                    db.close();
                } catch (Throwable ignored) {
                }
            }
        }
        return false;
    }

    /** 当前数据库里该 class 有多少行 */
    public static int rowCount(Context ctx, String cls) {
        File f = ctx.getDatabasePath(DB);
        if (!f.exists() || cls == null || cls.length() == 0) {
            return -1;
        }
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            Cursor c = db.rawQuery("SELECT count(*) FROM apps_data WHERE class_name=?",
                    new String[]{cls});
            int n = -1;
            if (c != null) {
                if (c.moveToFirst()) {
                    n = c.getInt(0);
                }
                c.close();
            }
            return n;
        } catch (Throwable e) {
            return -1;
        } finally {
            if (db != null) {
                try {
                    db.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static boolean isProtected(String cls) {
        for (String p : PROTECTED) {
            if (p.equals(cls)) {
                return true;
            }
        }
        return false;
    }
}
