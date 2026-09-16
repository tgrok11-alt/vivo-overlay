package com.vivooverlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;

public class OverlayService extends Service {

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP  = "ACTION_STOP";
    private static final String CHANNEL_ID  = "overlay_channel";
    private static final int    NOTIF_ID    = 1;
    private static final long   UPDATE_MS   = 1000; // update tiap 1 detik

    private WindowManager   windowManager;
    private View            overlayView;
    private WindowManager.LayoutParams params;

    private TextView tvCpu, tvRam, tvTemp;
    private ImageButton btnHide;
    private View        contentPanel;
    private boolean     isContentVisible = true;

    private final Handler  handler  = new Handler(Looper.getMainLooper());
    private final Runnable updater  = new Runnable() {
        @Override public void run() {
            updateStats();
            handler.postDelayed(this, UPDATE_MS);
        }
    };

    // Drag state
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        if (ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_START.equals(intent.getAction())) {
            startForeground(NOTIF_ID, buildNotification());
            showOverlay();
        }

        return START_STICKY;
    }

    private void showOverlay() {
        if (overlayView != null) return;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null);

        tvCpu        = overlayView.findViewById(R.id.tvCpu);
        tvRam        = overlayView.findViewById(R.id.tvRam);
        tvTemp       = overlayView.findViewById(R.id.tvTemp);
        btnHide      = overlayView.findViewById(R.id.btnHide);
        contentPanel = overlayView.findViewById(R.id.contentPanel);

        // Pojok kanan atas
        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 16;
        params.y = getStatusBarHeight() + 8;

        // Drag handler
        overlayView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX      = params.x;
                    initialY      = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    // Gravity END → x bertambah ke kiri
                    params.x = initialX - (int)(event.getRawX() - initialTouchX);
                    params.y = initialY + (int)(event.getRawY() - initialTouchY);
                    windowManager.updateViewLayout(overlayView, params);
                    return true;
            }
            return false;
        });

        // Tombol hide/show
        btnHide.setOnClickListener(v -> {
            isContentVisible = !isContentVisible;
            contentPanel.setVisibility(isContentVisible ? View.VISIBLE : View.GONE);
            btnHide.setText(isContentVisible ? "−" : "+");
        });

        windowManager.addView(overlayView, params);
        handler.post(updater);
    }

    private void updateStats() {
        if (overlayView == null) return;

        // CPU
        float cpu = CpuReader.getCpuUsage();
        tvCpu.setText(String.format("CPU  %.0f%%", cpu));
        tvCpu.setTextColor(getColorForPercent(cpu));

        // RAM
        float[] ram = RamReader.getRamUsage(this);
        tvRam.setText(String.format("RAM  %.1f/%.1fGB", ram[0], ram[1]));

        // Suhu baterai
        float temp = BatteryReader.getBatteryTemp(this);
        tvTemp.setText(String.format("TEMP %.0f°C", temp));
        tvTemp.setTextColor(temp >= 45 ? 0xFFFF4444 : temp >= 38 ? 0xFFFFAA00 : 0xFF00FF88);
    }

    private int getColorForPercent(float pct) {
        if (pct >= 85) return 0xFFFF4444; // merah
        if (pct >= 60) return 0xFFFFAA00; // kuning
        return 0xFF00FF88;                 // hijau
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resId  = Resources.getSystem().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) result = Resources.getSystem().getDimensionPixelSize(resId);
        return result;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updater);
        if (overlayView != null && windowManager != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
        }
    }

    private Notification buildNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Overlay Service", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
        }

        Intent stopIntent = new Intent(this, OverlayService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPi = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("System Overlay aktif")
            .setContentText("Tap untuk matikan overlay")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .addAction(android.R.drawable.ic_delete, "Matikan", stopPi)
            .setOngoing(true)
            .build();
    }
}
