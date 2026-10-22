package com.tricheer.guard;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

/**
 * 守护线程
 *   - 轮询间隔：elapsedRealtime（含休眠，醒来立即补一次检查）
 *   - DiPlay 拉起延迟：uptimeMillis（不含休眠，休眠期间不计时）
 *   - DiPlay 拉起：先服务；解析失败/启动后 3 秒仍未起来才升级为 Activity
 */
public final class Guard {

    private static final String TAG = "K218Guard";
    private static final String KEY_AP = "wifi_ap_state";
    private static final String DIPLAY_PKG = "com.shihab.diplay";
    private static final String DIPLAY_SVC = "com.shilapi.xcertplay.DiPlaySessionService";
    private static final String DIPLAY_ACT = "com.shilapi.xcertplay.DiPlayActivity";

    private static final long VERIFY_MS = 3000L;

    private static volatile boolean sStarted = false;
    private static volatile long sPendingSince = 0L;   // startService 之后待复验的起点
    private static volatile boolean sReportedMissing = false;

    private Guard() {
    }

    public static void start(Context ctx) {
        if (sStarted) {
            return;
        }
        sStarted = true;
        final Context app = ctx.getApplicationContext();
        if (app instanceof android.app.Application) {
            TileIcons.install((android.app.Application) app);
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                loop(app);
            }
        }, "k218-guard");
        t.setDaemon(true);
        t.start();
    }

    private static void loop(Context app) {
        Log.i(TAG, "guard thread started");
        long nextAp = 0L;
        long nextDp = 0L;
        long missingSince = 0L;
        boolean seeded = false;
        while (true) {
            try {
                if (!seeded) {
                    seeded = DbSeed.ensure(app);
                }
                Cfg c = Cfg.load(app);
                long wall = SystemClock.elapsedRealtime();
                long up = SystemClock.uptimeMillis();

                if (c.apOn && wall >= nextAp) {
                    nextAp = wall + c.apIvl * 1000L;
                    keepAp(app);
                }

                if (c.dpOn) {
                    if (wall >= nextDp) {
                        nextDp = wall + c.dpIvl * 1000L;
                        if (isAlive(app, DIPLAY_PKG)) {
                            if (missingSince != 0L) {
                                Log.i(TAG, "DiPlay is back");
                            }
                            missingSince = 0L;
                            sReportedMissing = false;
                        } else if (missingSince == 0L || up < missingSince) {
                            missingSince = up;
                            Log.i(TAG, "DiPlay missing, wait " + c.dpDelay + "s");
                        }
                    }
                    if (missingSince != 0L && up - missingSince >= c.dpDelay * 1000L) {
                        startDiPlay(app);
                        missingSince = 0L;
                        nextDp = wall + c.dpIvl * 1000L;
                    }
                }

                // 复验：startService 之后 3 秒还没起来 → 升级为启动 Activity
                if (sPendingSince != 0L) {
                    if (isAlive(app, DIPLAY_PKG)) {
                        Log.i(TAG, "DiPlay startService verified");
                        sPendingSince = 0L;
                        sReportedMissing = false;
                    } else if (up - sPendingSince >= VERIFY_MS) {
                        sPendingSince = 0L;
                        launchActivity(app);
                    }
                }
            } catch (Throwable e) {
                Log.w(TAG, "loop error", e);
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static void keepAp(Context app) {
        try {
            String cur = Settings.Global.getString(app.getContentResolver(), KEY_AP);
            if (!"on".equals(cur)) {
                Settings.Global.putString(app.getContentResolver(), KEY_AP, "on");
                Log.i(TAG, "wifi_ap_state: " + cur + " -> on");
            }
        } catch (Throwable e) {
            Log.w(TAG, "ap state failed", e);
        }
    }

    private static void startDiPlay(Context app) {
        if (!isInstalled(app, DIPLAY_PKG)) {
            if (!sReportedMissing) {
                sReportedMissing = true;
                Log.w(TAG, "DiPlay not installed, skip");
            }
            return;
        }
        sReportedMissing = false;

        Intent svc = new Intent();
        svc.setComponent(new ComponentName(DIPLAY_PKG, DIPLAY_SVC));
        boolean hasSvc = false;
        try {
            ResolveInfo ri = app.getPackageManager().resolveService(svc, 0);
            hasSvc = ri != null;
        } catch (Throwable ignored) {
        }

        if (!hasSvc) {
            Log.w(TAG, "DiPlay service not found, launch activity directly");
            launchActivity(app);
            return;
        }

        try {
            app.startService(svc);
            sPendingSince = SystemClock.uptimeMillis();
            Log.i(TAG, "DiPlay startService (verify in 3s)");
        } catch (Throwable e) {
            Log.w(TAG, "startService failed", e);
            launchActivity(app);
        }
    }

    private static void launchActivity(Context app) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setComponent(new ComponentName(DIPLAY_PKG, DIPLAY_ACT));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            app.startActivity(i);
            Log.i(TAG, "DiPlay startActivity (escalated)");
            sPendingSince = 0L;
        } catch (Throwable e) {
            Log.w(TAG, "startActivity failed", e);
        }
    }

    private static boolean isInstalled(Context c, String pkg) {
        try {
            c.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private static boolean isAlive(Context app, String pkg) {
        try {
            ActivityManager am = (ActivityManager) app.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) {
                return false;
            }
            java.util.List<ActivityManager.RunningAppProcessInfo> list = am.getRunningAppProcesses();
            if (list != null) {
                for (ActivityManager.RunningAppProcessInfo p : list) {
                    if (pkg.equals(p.processName)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
