package com.example.autorotate.UI;

import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.autorotate.R;
import com.example.autorotate.Util.OperationMode;
import com.example.autorotate.Util.OperationModeStore;
import com.example.autorotate.Util.RotationBackend;

import rikka.shizuku.Shizuku;

public class SettingsFragment extends PreferenceFragmentCompat {

    private static final int REQUEST_CODE_SHIZUKU_PERMISSION = 1001;

    private ListPreference modePreference;
    private Preference statusPreference;

    private final Shizuku.OnRequestPermissionResultListener permissionListener = this::onShizukuPermissionResult;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey);

        OperationModeStore.ensurePreferredMode(requireContext());

        Shizuku.addRequestPermissionResultListener(permissionListener);

        modePreference = findPreference("operation_mode");
        statusPreference = findPreference("operation_mode_status");

        if (modePreference != null) {
            modePreference.setValue(OperationModeStore.get(requireContext()).getValue());
            modePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                OperationMode selectedMode = OperationMode.fromValue(String.valueOf(newValue));
                OperationModeStore.set(requireContext(), selectedMode);

                if (selectedMode == OperationMode.SHIZUKU && !hasShizukuPermission()) {
                    requestShizukuPermission();
                }

                updateBackendStatus();
                return true;
            });
        }

        updateBackendStatus();
    }

    @Override
    public void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBackendStatus();
    }

    private boolean hasShizukuPermission() {
        try {
            return !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable tr) {
            return false;
        }
    }

    private boolean requestShizukuPermission() {
        try {
            if (Shizuku.isPreV11()) {
                return false;
            }

            if (Shizuku.getBinder() == null) {
                return false;
            }

            if (Shizuku.shouldShowRequestPermissionRationale()) {
                return false;
            }

            Shizuku.requestPermission(REQUEST_CODE_SHIZUKU_PERMISSION);
            return true;
        } catch (Throwable tr) {
            return false;
        }
    }

    private void onShizukuPermissionResult(int requestCode, int grantResult) {
        if (requestCode != REQUEST_CODE_SHIZUKU_PERMISSION) {
            return;
        }

        if (grantResult == PackageManager.PERMISSION_GRANTED && modePreference != null) {
            modePreference.setValue(OperationMode.SHIZUKU.getValue());
        }

        updateBackendStatus();
    }

    private void updateBackendStatus() {
        if (statusPreference != null) {
            statusPreference.setSummary(RotationBackend.describeBackend(requireContext()));
        }
    }
}