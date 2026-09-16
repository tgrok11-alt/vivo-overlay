package com.vivooverlay;

import java.io.BufferedReader;
import java.io.FileReader;

public class CpuReader {

    private static long prevIdle  = 0;
    private static long prevTotal = 0;

    public static float getCpuUsage() {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/stat"))) {
            String line = br.readLine(); // baris pertama: cpu total
            if (line == null) return 0f;

            String[] parts = line.trim().split("\\s+");
            // format: cpu user nice system idle iowait irq softirq steal guest guest_nice
            long user    = Long.parseLong(parts[1]);
            long nice    = Long.parseLong(parts[2]);
            long system  = Long.parseLong(parts[3]);
            long idle    = Long.parseLong(parts[4]);
            long iowait  = Long.parseLong(parts[5]);
            long irq     = Long.parseLong(parts[6]);
            long softirq = Long.parseLong(parts[7]);

            long totalIdle  = idle + iowait;
            long totalBusy  = user + nice + system + irq + softirq;
            long total      = totalIdle + totalBusy;

            long diffIdle  = totalIdle - prevIdle;
            long diffTotal = total     - prevTotal;

            prevIdle  = totalIdle;
            prevTotal = total;

            if (diffTotal == 0) return 0f;
            float usage = 100f * (1f - (float) diffIdle / diffTotal);
            return Math.max(0f, Math.min(100f, usage));

        } catch (Exception e) {
            return 0f;
        }
    }
}
