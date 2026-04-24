package com.first.teja2.realautorotate.Service;

/*
Created By
Bala Guna Teja Karlapudi
 */

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.first.teja2.realautorotate.Util.UsageStatsHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashSet;

public class realAutorotateService extends Service {

    private static final long FOREGROUND_LOOKBACK_WINDOW_MILLIS = 2000L;
    private static final long FOREGROUND_POLL_INTERVAL_MILLIS = 1000L;

    HashSet<String> selectedApps;
    private HandlerThread foregroundCheckerThread;
    private Handler handler;
    private Runnable foregroundAppChecker;
    private volatile boolean isServiceActive;

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
            startForegroundAppChecks();
        } else
            this.stopSelf();

        return START_STICKY;
    }

    private void startForegroundAppChecks() {
        if (foregroundCheckerThread == null || !foregroundCheckerThread.isAlive()) {
            foregroundCheckerThread = new HandlerThread("ForegroundAppChecker");
            foregroundCheckerThread.start();
        }
        handler = new Handler(foregroundCheckerThread.getLooper());
        isServiceActive = true;

        if (foregroundAppChecker != null) {
            handler.removeCallbacks(foregroundAppChecker);
        }

        foregroundAppChecker = new Runnable() {
            @Override
            public void run() {
                String packageName = UsageStatsHelper.getForegroundAppPackage(realAutorotateService.this, FOREGROUND_LOOKBACK_WINDOW_MILLIS);
                if (packageName != null && selectedApps.contains(packageName)) {
                    Settings.System.putInt(realAutorotateService.this.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1);
                } else {
                    Settings.System.putInt(realAutorotateService.this.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                }
                if (isServiceActive && handler != null) {
                    handler.postDelayed(this, FOREGROUND_POLL_INTERVAL_MILLIS);
                }
            }
        };
        handler.post(foregroundAppChecker);
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
        isServiceActive = false;
        if (handler != null && foregroundAppChecker != null) {
            handler.removeCallbacks(foregroundAppChecker);
        }
        if (foregroundCheckerThread != null) {
            foregroundCheckerThread.quitSafely();
            foregroundCheckerThread = null;
        }
        foregroundAppChecker = null;
        handler = null;
    }
}
