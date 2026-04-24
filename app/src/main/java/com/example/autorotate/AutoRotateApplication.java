package com.example.autorotate;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

import com.example.autorotate.Util.OperationModeStore;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuProvider;

public class AutoRotateApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);

        ShizukuProvider.requestBinderForNonProviderProcess(this);
        OperationModeStore.ensurePreferredMode(this);
        Shizuku.addBinderReceivedListenerSticky(() -> OperationModeStore.ensurePreferredMode(this));
    }
}
