package com.vivooverlay;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class BatteryReader {

    public static float getBatteryTemp(Context context) {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            if (batteryStatus == null) return 0f;
            int temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            return temp / 10f; // dalam derajat Celsius
        } catch (Exception e) {
            return 0f;
        }
    }
}
