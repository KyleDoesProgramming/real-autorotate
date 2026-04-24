package com.example.autorotate.Model;

/*
Created By
Bala Guna Teja Karlapudi
 */


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class AppsInfo {

    public static final int MODE_FOLLOW_DEFAULT = 0;
    public static final int MODE_LOCK_ROTATION = 1;
    public static final int MODE_ALLOW_ROTATION = 2;
    public static final int MODE_FORCE_LOCK = MODE_LOCK_ROTATION;
    public static final int MODE_FORCE_ROTATE = MODE_ALLOW_ROTATION;

    String appName, appPackageName;
    boolean defaultEntry;
    int rotationMode = MODE_FOLLOW_DEFAULT;
    List<Integer> specificRotations = new ArrayList<>(Arrays.asList(0));

    public AppsInfo(String appName, String appPackageName) {
        this(appName, appPackageName, MODE_FOLLOW_DEFAULT);
    }

    public AppsInfo(String appName, String appPackageName, int rotationMode) {
        this.appName = appName;
        this.appPackageName = appPackageName;
        this.rotationMode = rotationMode;
    }

    public static AppsInfo createDefaultEntry() {
        AppsInfo appsInfo = new AppsInfo("Default", "", MODE_LOCK_ROTATION);
        appsInfo.defaultEntry = true;
        appsInfo.specificRotations = new ArrayList<>(Arrays.asList(0));
        return appsInfo;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppPackageName() {
        return appPackageName;
    }

    public boolean isDefaultEntry() {
        return defaultEntry;
    }

    public void setDefaultEntry(boolean defaultEntry) {
        this.defaultEntry = defaultEntry;
    }

    public int getRotationMode() {
        return rotationMode;
    }

    public void setRotationMode(int rotationMode) {
        this.rotationMode = rotationMode;
    }

    public List<Integer> getSpecificRotations() {
        if (specificRotations == null) {
            specificRotations = new ArrayList<>();
        }
        return specificRotations;
    }

    public void setSpecificRotations(List<Integer> specificRotations) {
        this.specificRotations = specificRotations != null ? new ArrayList<>(specificRotations) : new ArrayList<>();
    }

}
