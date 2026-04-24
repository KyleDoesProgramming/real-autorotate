package com.example.autorotate.Util;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.NonNull;

public final class PermissionGate {

    private PermissionGate() {
    }

    public static boolean needsWriteSettings(@NonNull Context context) {
        OperationMode mode = OperationModeStore.get(context);
        if (mode == OperationMode.ACCESSIBILITY) {
            return true;
        }

        return !RotationBackend.canUsePrivileged(context);
    }

    public static boolean hasRequiredPermissions(@NonNull Context context) {
        return UsageStatsHelper.hasUsageStatsPermission(context)
                && UsageStatsHelper.hasBatteryOptimizationExemption(context)
                && (!needsWriteSettings(context) || Settings.System.canWrite(context));
    }
}