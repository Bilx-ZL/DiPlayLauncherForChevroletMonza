package com.tricheer.guard;

import android.content.Context;
import android.content.SharedPreferences;

/** 守护配置（SharedPreferences） */
public final class Cfg {

    private static final String SP = "guard_cfg";

    public static final String K_AP_ON = "ap_on";
    public static final String K_DP_ON = "dp_on";
    public static final String K_AP_IVL = "ap_ivl";
    public static final String K_DP_IVL = "dp_ivl";
    public static final String K_DP_DELAY = "dp_delay";

    // ---- 默认值 ----
    public static final boolean DEF_AP_ON = false;
    public static final boolean DEF_DP_ON = false;
    public static final int DEF_AP_IVL = 5;
    public static final int DEF_DP_IVL = 5;
    public static final int DEF_DP_DELAY = 3;

    public boolean apOn = DEF_AP_ON;
    public boolean dpOn = DEF_DP_ON;
    public int apIvl = DEF_AP_IVL;
    public int dpIvl = DEF_DP_IVL;
    public int dpDelay = DEF_DP_DELAY;

    public static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(SP, Context.MODE_PRIVATE);
    }

    public static Cfg load(Context c) {
        SharedPreferences p = sp(c);
        Cfg g = new Cfg();
        g.apOn = p.getBoolean(K_AP_ON, DEF_AP_ON);
        g.dpOn = p.getBoolean(K_DP_ON, DEF_DP_ON);
        g.apIvl = Math.max(1, p.getInt(K_AP_IVL, DEF_AP_IVL));
        g.dpIvl = Math.max(1, p.getInt(K_DP_IVL, DEF_DP_IVL));
        g.dpDelay = Math.max(0, p.getInt(K_DP_DELAY, DEF_DP_DELAY));
        return g;
    }
}
