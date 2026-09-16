package com.vivooverlay;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStart = findViewById(R.id.btnStart);
        Button btnStop = findViewById(R.id.btnStop);

        btnStart.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                showPermissionDialog();
            } else {
                startOverlay();
            }
        });

        btnStop.setOnClickListener(v -> stopOverlay());
    }

    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Izin Diperlukan")
            .setMessage("App ini butuh izin 'Tampil di atas app lain' untuk menampilkan overlay.\n\nKlik OK lalu aktifkan izinnya.")
            .setPositiveButton("OK", (d, w) -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            })
            .setNegativeButton("Batal", null)
            .show();
    }

    private void startOverlay() {
        Intent intent = new Intent(this, OverlayService.class);
        intent.setAction(OverlayService.ACTION_START);
        startForegroundService(intent);
        Toast.makeText(this, "Overlay aktif", Toast.LENGTH_SHORT).show();
    }

    private void stopOverlay() {
        Intent intent = new Intent(this, OverlayService.class);
        intent.setAction(OverlayService.ACTION_STOP);
        startService(intent);
        Toast.makeText(this, "Overlay dimatikan", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                startOverlay();
            } else {
                Toast.makeText(this, "Izin ditolak, overlay tidak bisa aktif", Toast.LENGTH_LONG).show();
            }
        }
    }
}
