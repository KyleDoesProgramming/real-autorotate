package com.example.autorotate.Util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import rikka.shizuku.Shizuku;

public final class OperationModeStore {

    private static final String KEY = "operation_mode";

    private OperationModeStore() {
    }

    @NonNull
    public static OperationMode get(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return OperationMode.fromValue(prefs.getString(KEY, null));
    }

    public static void set(@NonNull Context context, @NonNull OperationMode mode) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(KEY, mode.getValue())
                .apply();
    }

    public static void ensurePreferredMode(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.contains(KEY)) {
            return;
        }

        prefs.edit().putString(KEY, OperationMode.AUTO.getValue()).apply();
    }

    @NonNull
    public static OperationMode detectMode() {
        try {
            if (Shizuku.getBinder() == null) {
                return OperationMode.ACCESSIBILITY;
            }

            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                return OperationMode.ACCESSIBILITY;
            }

            int uid = Shizuku.getUid();
            if (uid == 0) {
                return OperationMode.ROOT;
            }

            if (uid == 2000) {
                return OperationMode.SHIZUKU;
            }
        } catch (Throwable ignored) {
        }

        return OperationMode.ACCESSIBILITY;
    }
}