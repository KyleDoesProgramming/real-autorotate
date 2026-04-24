package com.first.teja2.realautorotate.Service;

/*
Created By
Bala Guna Teja Karlapudi
 */

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.first.teja2.realautorotate.Util.UsageStatsHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashSet;

public class realAutorotateService extends Service {

    HashSet<String> selectedApps;
    private Handler handler;
    private Runnable foregroundAppChecker;

    public realAutorotateService() {
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {

        String jsonData = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString("selectedPackages", "Data Unavailable");

        Gson gson = new Gson();
        Type type = new TypeToken<HashSet<String>>() {
        }.getType();
        selectedApps = gson.fromJson(jsonData, type);
        if (selectedApps == null) {
            selectedApps = new HashSet<>();
        }

        int status = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getInt("status", -1);

        if (Settings.System.canWrite(realAutorotateService.this) && !selectedApps.isEmpty() && status == 1) {
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            if (foregroundAppChecker != null) {
                handler.removeCallbacks(foregroundAppChecker);
            }
            foregroundAppChecker = new Runnable() {
                @Override
                public void run() {
                    String packageName = UsageStatsHelper.getForegroundAppPackage(realAutorotateService.this, 10000L);
                    if (packageName != null && selectedApps.contains(packageName)) {
                        Settings.System.putInt(realAutorotateService.this.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1);
                    } else {
                        Settings.System.putInt(realAutorotateService.this.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                    }
                    handler.postDelayed(this, 1000L);
                }
            };
            handler.post(foregroundAppChecker);
        } else
            this.stopSelf();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && foregroundAppChecker != null) {
            handler.removeCallbacks(foregroundAppChecker);
        }
    }
}
