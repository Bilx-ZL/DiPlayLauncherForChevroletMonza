package com.tricheer.guard.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** 两个界面底部的项目说明 / 免责声明 / 隐私说明 */
public final class Footer {

    public static final String URL =
            "https://github.com/Bilx-ZL/DiPlayLauncherForChevroletMonza";

    private static final int FG = 0xFFEDEFF2;
    private static final int SUB = 0xFF7E8792;
    private static final int ACCENT = 0xFF6FA8FF;
    private static final int LINE = 0xFF262C34;

    private Footer() {
    }

    public static View build(final Activity a) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        int top = dp(a, 28);
        box.setPadding(0, top, 0, dp(a, 24));

        View line = new View(a);
        line.setBackgroundColor(LINE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(a, 1));
        lp.bottomMargin = dp(a, 16);
        box.addView(line, lp);

        // 项目主页
        LinearLayout homeRow = new LinearLayout(a);
        homeRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView label = small(a, "项目主页　", SUB, false);
        homeRow.addView(label);

        TextView link = small(a, URL, ACCENT, false);
        link.setSingleLine(true);
        link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLinkMenu(a);
            }
        });
        homeRow.addView(link);
        box.addView(homeRow);

        box.addView(small(a, "\n新增功能仅用于辅助以 DiPlay 替代原车 CarPlay，提供更流畅的画面和多指触控体验。", SUB, false));
        box.addView(small(a, "修改后的车机桌面仅适用于 2019–2021 款科鲁泽原装 Android 车机，仅本人自用，未在其他机型上测试。", SUB, false));

        TextView warn = small(a, "免责声明", 0xFFB9C2CC, true);
        warn.setPadding(0, dp(a, 14), 0, 0);
        box.addView(warn);
        box.addView(small(a, "非官方修改，与雪佛兰、DiPlay 及车机厂商均无关联。安装或刷写存在风险，可能导致车机异常，请自行评估并承担后果；请在安全环境下操作，行车中请勿操作本界面。", SUB, false));

        TextView priv = small(a, "隐私说明", 0xFFB9C2CC, true);
        priv.setPadding(0, dp(a, 10), 0, 0);
        box.addView(priv);
        box.addView(small(a, "新增功能不联网、不上传、不收集任何数据；仅在本机读写系统设置项 wifi_ap_state 与自身配置文件，数据全部保留在车机本地。应用原有权限与功能未作改动。", SUB, false));

        return box;
    }

    private static void showLinkMenu(final Activity a) {
        new AlertDialog.Builder(a)
                .setTitle("项目主页")
                .setItems(new String[]{"打开链接", "复制链接"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0) {
                            try {
                                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(URL));
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                a.startActivity(i);
                            } catch (Throwable e) {
                                toast(a, "没有可用的浏览器");
                            }
                        } else {
                            try {
                                ClipboardManager cm =
                                        (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
                                cm.setPrimaryClip(ClipData.newPlainText("url", URL));
                                toast(a, "已复制");
                            } catch (Throwable e) {
                                toast(a, "复制失败");
                            }
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private static void toast(Context c, String s) {
        try {
            Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }

    private static TextView small(Activity a, String text, int color, boolean bold) {
        TextView t = new TextView(a);
        t.setText(text);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t.setLineSpacing(0, 1.35f);
        if (bold) {
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            t.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        return t;
    }

    private static int dp(Context c, int v) {
        return (int) (v * c.getResources().getDisplayMetrics().density);
    }
}
