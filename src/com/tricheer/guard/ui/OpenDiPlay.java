package com.tricheer.guard.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.widget.Toast;

/**
 * 桌面入口：打开 DiPlay（无界面，打开即退出）
 * 启动 Intent 与「全部应用」列表完全一致：
 *   MAIN + CATEGORY_LAUNCHER + 解析出的组件 + FLAG_ACTIVITY_NEW_TASK
 * 这样 DiPlay 已在镜像时会被切回原任务，而不是新建实例。
 */
public class OpenDiPlay extends Activity {

    private static final String PKG = "com.shihab.diplay";
    private static final String ACT = "com.shilapi.xcertplay.DiPlayActivity";
    private static final String SVC = "com.shilapi.xcertplay.DiPlaySessionService";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!isInstalled()) {
            try {
                Toast.makeText(this, "未安装 DiPlay", Toast.LENGTH_LONG).show();
            } catch (Throwable ignored) {
            }
            finish();
            return;
        }
        boolean ok = false;
        try {
            startActivity(launcherIntent());
            ok = true;
        } catch (Throwable ignored) {
        }
        if (!ok) {
            try {
                startService(new Intent().setComponent(new ComponentName(PKG, SVC)));
                ok = true;
            } catch (Throwable ignored) {
            }
        }
        if (!ok) {
            try {
                Toast.makeText(this, "DiPlay 未安装", Toast.LENGTH_SHORT).show();
            } catch (Throwable ignored) {
            }
        }
        finish();
    }

    private boolean isInstalled() {
        try {
            getPackageManager().getPackageInfo(PKG, 0);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /** 与 AllAppsActivity.launch() 完全一致的构造方式 */
    private Intent launcherIntent() {
        String pkg = PKG;
        String cls = ACT;
        try {
            Intent probe = new Intent(Intent.ACTION_MAIN);
            probe.addCategory(Intent.CATEGORY_LAUNCHER);
            probe.setPackage(PKG);
            ResolveInfo ri = getPackageManager().resolveActivity(probe, 0);
            if (ri != null && ri.activityInfo != null) {
                pkg = ri.activityInfo.packageName;
                cls = ri.activityInfo.name;
            }
        } catch (Throwable ignored) {
        }
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.setClassName(pkg, cls);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }
}
