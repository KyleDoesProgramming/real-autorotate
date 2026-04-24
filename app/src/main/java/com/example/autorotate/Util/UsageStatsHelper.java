package com.example.autorotate.Util;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.os.PowerManager;

import java.util.List;

public final class UsageStatsHelper {

    private UsageStatsHelper() {
    }

    public static boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOpsManager == null) {
            return false;
        }

        int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public static boolean hasBatteryOptimizationExemption(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return powerManager != null && powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    public static String getForegroundAppPackage(Context context, long lookbackWindowMillis) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usageStatsManager == null) {
            return null;
        }

        long endTime = System.currentTimeMillis();
        long startTime = endTime - lookbackWindowMillis;

        UsageEvents usageEvents = usageStatsManager.queryEvents(startTime, endTime);
        UsageEvents.Event event = new UsageEvents.Event();
        String foregroundPackage = null;
        long latestTimestamp = 0L;

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            int eventType = event.getEventType();
            if (eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                    || eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                String packageName = event.getPackageName();
                if (packageName != null && event.getTimeStamp() >= latestTimestamp) {
                    latestTimestamp = event.getTimeStamp();
                    foregroundPackage = packageName;
                }
            }
        }

        if (foregroundPackage != null) {
            return foregroundPackage;
        }

        List<UsageStats> usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, endTime);
        if (usageStats == null || usageStats.isEmpty()) {
            return null;
        }

        UsageStats latestUsageStats = null;
        for (UsageStats stat : usageStats) {
            if (latestUsageStats == null || stat.getLastTimeUsed() > latestUsageStats.getLastTimeUsed()) {
                latestUsageStats = stat;
            }
        }

        return latestUsageStats != null ? latestUsageStats.getPackageName() : null;
    }
}
