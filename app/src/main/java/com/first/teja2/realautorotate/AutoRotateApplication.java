package com.first.teja2.realautorotate;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

public class AutoRotateApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
