package com.vivooverlay;

import android.app.ActivityManager;
import android.content.Context;

public class RamReader {

    /**
     * @return float[2] → [used GB, total GB]
     */
    public static float[] getRamUsage(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);

        float totalGb = mi.totalMem  / 1_073_741_824f; // bytes → GB
        float freeGb  = mi.availMem  / 1_073_741_824f;
        float usedGb  = totalGb - freeGb;

        return new float[]{usedGb, totalGb};
    }
}
