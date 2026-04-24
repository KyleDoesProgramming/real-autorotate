package com.example.autorotate.Util;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.annotation.NonNull;

public final class RotationSettings {

    private RotationSettings() {
    }

    public static boolean apply(@NonNull Context context, int accelerometerRotation, int userRotation) {
        ContentResolver resolver = context.getContentResolver();
        boolean changed = false;

        int currentAccelerometerRotation = Settings.System.getInt(
                resolver,
                Settings.System.ACCELEROMETER_ROTATION,
                -1);
        if (currentAccelerometerRotation != accelerometerRotation) {
            Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, accelerometerRotation);
            changed = true;
        }

        int currentUserRotation = Settings.System.getInt(
                resolver,
                Settings.System.USER_ROTATION,
                -1);
        if (currentUserRotation != userRotation) {
            Settings.System.putInt(resolver, Settings.System.USER_ROTATION, userRotation);
            changed = true;
        }

        return changed;
    }
}