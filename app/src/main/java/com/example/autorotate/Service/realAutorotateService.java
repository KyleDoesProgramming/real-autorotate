package com.example.autorotate.Service;

/*
Created By
Bala Guna Teja Karlapudi
 */

import android.app.Service;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.OrientationEventListener;
import android.view.Surface;

import com.example.autorotate.Model.AppsInfo;
import com.example.autorotate.R;
import com.example.autorotate.Util.UsageStatsHelper;
import com.example.autorotate.Util.PermissionGate;
import com.example.autorotate.Util.RotationBackend;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class realAutorotateService extends Service {

    private static final long FOREGROUND_LOOKBACK_WINDOW_MILLIS = 10000L;
    private static final long FOREGROUND_POLL_INTERVAL_MILLIS = 1500L;
    private static final long TRANSIENT_PACKAGE_GRACE_MILLIS = 4000L;

    private HashMap<String, AppsInfo> appRules;
    private AppsInfo defaultRule;
    private volatile AppsInfo activeRule;
    private OrientationEventListener orientationListener;
    private int lastOrientationDegrees = OrientationEventListener.ORIENTATION_UNKNOWN;
    private HandlerThread foregroundCheckerThread;
    private Handler handler;
    private Runnable foregroundAppChecker;
    private volatile boolean isServiceActive;
    private volatile String currentPackageName;
    private String homePackageName;
    private long transientPackageSinceMillis = -1L;
    private Integer lastRotationState;
    private Integer lastUserRotation;

    public realAutorotateService() {
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {

        String jsonData = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString(getString(R.string.selectedApps), "Data Unavailable");

        Gson gson = new Gson();
        appRules = new HashMap<>();
        defaultRule = AppsInfo.createDefaultEntry();

        if (!"Data Unavailable".equals(jsonData)) {
            try {
                Type type = new TypeToken<List<AppsInfo>>() {
                }.getType();
                List<AppsInfo> savedApps = gson.fromJson(jsonData, type);
                if (savedApps != null) {
                    for (AppsInfo appsInfo : savedApps) {
                        if (appsInfo == null) {
                            continue;
                        }

                        if (appsInfo.isDefaultEntry() || appsInfo.getAppPackageName() == null || appsInfo.getAppPackageName().isEmpty()) {
                            defaultRule = appsInfo;
                            if (defaultRule.getRotationMode() == AppsInfo.MODE_FOLLOW_DEFAULT) {
                                defaultRule.setRotationMode(AppsInfo.MODE_LOCK_ROTATION);
                            }
                            continue;
                        }

                        appRules.put(appsInfo.getAppPackageName(), appsInfo);
                    }
                }
            } catch (RuntimeException ignored) {
            }
        }

        int status = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getInt("status", -1);

        if (status == 1 && PermissionGate.hasRequiredPermissions(realAutorotateService.this)) {
            startForegroundAppChecks();
        } else {
            this.stopSelf();
        }

        return START_STICKY;
    }

    private void startForegroundAppChecks() {
        if (foregroundCheckerThread == null || !foregroundCheckerThread.isAlive()) {
            foregroundCheckerThread = new HandlerThread("ForegroundAppChecker");
            foregroundCheckerThread.start();
        }
        handler = new Handler(foregroundCheckerThread.getLooper());
        isServiceActive = true;
        currentPackageName = null;
        homePackageName = resolveHomePackageName();
        transientPackageSinceMillis = -1L;
        lastRotationState = null;
        lastUserRotation = null;

        if (orientationListener == null) {
            orientationListener = new OrientationEventListener(getApplicationContext()) {
                @Override
                public void onOrientationChanged(int orientation) {
                    if (!isServiceActive) {
                        return;
                    }

                    lastOrientationDegrees = orientation;
                    applyRotationRule();
                }
            };
        }

        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable();
        }

        RotationBackend.ensureBound(this);

        if (foregroundAppChecker != null) {
            handler.removeCallbacks(foregroundAppChecker);
        }

        foregroundAppChecker = new Runnable() {
            @Override
            public void run() {
                String packageName = UsageStatsHelper.getForegroundAppPackage(realAutorotateService.this, FOREGROUND_LOOKBACK_WINDOW_MILLIS);
                String effectivePackage = resolveEffectivePackage(packageName);
                if (effectivePackage != null) {
                    currentPackageName = effectivePackage;
                    activeRule = resolveRule(effectivePackage);
                } else {
                    currentPackageName = null;
                    activeRule = defaultRule;
                }
                applyRotationRule();
                if (isServiceActive && handler != null) {
                    handler.postDelayed(this, FOREGROUND_POLL_INTERVAL_MILLIS);
                }
            }
        };
        handler.post(foregroundAppChecker);
    }

    private String resolveEffectivePackage(String detectedPackage) {
        if (!isTransientPackage(detectedPackage)) {
            transientPackageSinceMillis = -1L;
            return detectedPackage;
        }

        long now = System.currentTimeMillis();
        if (transientPackageSinceMillis < 0L) {
            transientPackageSinceMillis = now;
        }

        if (!isTransientPackage(currentPackageName)
                && now - transientPackageSinceMillis <= TRANSIENT_PACKAGE_GRACE_MILLIS) {
            return currentPackageName;
        }

        return detectedPackage;
    }

    private boolean isTransientPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return true;
        }

        if ("com.android.systemui".equals(packageName)) {
            return true;
        }

        return homePackageName != null && homePackageName.equals(packageName);
    }

    private String resolveHomePackageName() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);

        ResolveInfo resolveInfo = getPackageManager().resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo == null || resolveInfo.activityInfo == null) {
            return null;
        }

        return resolveInfo.activityInfo.packageName;
    }

    private synchronized void applyRotationRule() {
        AppsInfo rule = activeRule != null ? activeRule : defaultRule;
        if (rule == null) {
            return;
        }

        List<Integer> allowedRotations = normalizeRotations(rule.getSpecificRotations());
        boolean allowAllRotations = rule.getRotationMode() == AppsInfo.MODE_ALLOW_ROTATION && hasAllRotations(allowedRotations);
        int targetAccelerometerState = allowAllRotations ? 1 : 0;
        int targetUserRotation = allowAllRotations ? Surface.ROTATION_0 : selectTargetUserRotation(allowedRotations);

        // Avoid unnecessary writes, but re-assert if system state drifted.
        if (lastRotationState != null && lastRotationState == targetAccelerometerState) {
            if (targetAccelerometerState == 1) {
                if (isRotationStateApplied(targetAccelerometerState, targetUserRotation)) {
                    return;
                }
            }

            if (lastUserRotation != null && lastUserRotation == targetUserRotation) {
                if (isRotationStateApplied(targetAccelerometerState, targetUserRotation)) {
                    return;
                }
            }
        }

        RotationBackend.apply(this, targetAccelerometerState, targetUserRotation);
        lastRotationState = targetAccelerometerState;
        lastUserRotation = targetUserRotation;
    }

    private boolean isRotationStateApplied(int targetAccelerometerState, int targetUserRotation) {
        try {
            int currentAccelerometerState = Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION,
                    -1
            );

            if (currentAccelerometerState != targetAccelerometerState) {
                return false;
            }

            if (targetAccelerometerState == 1) {
                return true;
            }

            int currentUserRotation = Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.USER_ROTATION,
                    -1
            );
            return currentUserRotation == targetUserRotation;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private List<Integer> normalizeRotations(List<Integer> rotations) {
        List<Integer> normalized = new ArrayList<>();
        if (rotations == null) {
            normalized.add(0);
            return normalized;
        }

        for (Integer rotation : rotations) {
            if (rotation == null) {
                continue;
            }
            if ((rotation == 0 || rotation == 90 || rotation == 180 || rotation == 270) && !normalized.contains(rotation)) {
                normalized.add(rotation);
            }
        }

        if (normalized.isEmpty()) {
            normalized.add(0);
        }
        return normalized;
    }

    private boolean hasAllRotations(List<Integer> rotations) {
        return rotations.contains(0) && rotations.contains(90) && rotations.contains(180) && rotations.contains(270);
    }

    private int selectTargetUserRotation(List<Integer> allowedRotations) {
        if (allowedRotations.isEmpty()) {
            return Surface.ROTATION_0;
        }

        if (lastOrientationDegrees == OrientationEventListener.ORIENTATION_UNKNOWN) {
            return toSurfaceRotation(allowedRotations.get(0));
        }

        int snappedRotation = snapOrientation(lastOrientationDegrees);
        if (allowedRotations.contains(snappedRotation)) {
            return toSurfaceRotation(snappedRotation);
        }

        int bestRotation = allowedRotations.get(0);
        int bestDistance = Integer.MAX_VALUE;
        for (Integer rotation : allowedRotations) {
            int distance = angularDistance(lastOrientationDegrees, rotation);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestRotation = rotation;
            }
        }

        return toSurfaceRotation(bestRotation);
    }

    private int snapOrientation(int orientationDegrees) {
        if (orientationDegrees >= 315 || orientationDegrees < 45) {
            return 0;
        }
        if (orientationDegrees < 135) {
            return 90;
        }
        if (orientationDegrees < 225) {
            return 180;
        }
        return 270;
    }

    private int angularDistance(int orientationDegrees, int candidateDegrees) {
        int rawDistance = Math.abs(orientationDegrees - candidateDegrees);
        return Math.min(rawDistance, 360 - rawDistance);
    }

    private AppsInfo resolveRule(String packageName) {
        if (packageName == null) {
            return defaultRule;
        }

        AppsInfo appsInfo = appRules.get(packageName);
        if (appsInfo != null && appsInfo.getRotationMode() != AppsInfo.MODE_FOLLOW_DEFAULT) {
            return appsInfo;
        }

        AppsInfo inferredRule = inferRuleFromManifest(packageName);
        if (inferredRule != null) {
            return inferredRule;
        }

        return defaultRule;
    }

    private AppsInfo inferRuleFromManifest(String packageName) {
        PackageManager packageManager = getPackageManager();
        Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
        if (launchIntent == null) {
            return null;
        }

        ResolveInfo resolveInfo = packageManager.resolveActivity(launchIntent, 0);
        if (resolveInfo == null || resolveInfo.activityInfo == null) {
            return null;
        }

        int orientation = resolveInfo.activityInfo.screenOrientation;
        AppsInfo inferredRule = new AppsInfo(packageName, packageName, AppsInfo.MODE_ALLOW_ROTATION);

        if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                || orientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                || orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            inferredRule.setSpecificRotations(Arrays.asList(90, 270));
            return inferredRule;
        }

        if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                || orientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                || orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
            inferredRule.setSpecificRotations(Arrays.asList(0, 180));
            return inferredRule;
        }

        return null;
    }

    private int toSurfaceRotation(int degrees) {
        if (degrees == 90) {
            return Surface.ROTATION_270;
        }
        if (degrees == 180) {
            return Surface.ROTATION_180;
        }
        if (degrees == 270) {
            return Surface.ROTATION_90;
        }
        return Surface.ROTATION_0;
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
        currentPackageName = null;
        if (handler != null && foregroundAppChecker != null) {
            handler.removeCallbacks(foregroundAppChecker);
        }
        if (foregroundCheckerThread != null) {
            foregroundCheckerThread.quitSafely();
            foregroundCheckerThread = null;
        }
        if (orientationListener != null) {
            orientationListener.disable();
        }
        lastRotationState = null;
        transientPackageSinceMillis = -1L;
        foregroundAppChecker = null;
        handler = null;
        activeRule = null;
    }
}
