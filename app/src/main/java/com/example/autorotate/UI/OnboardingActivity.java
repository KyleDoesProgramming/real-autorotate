package com.example.autorotate.UI;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.example.autorotate.R;
import com.example.autorotate.Util.PermissionGate;
import com.example.autorotate.Util.OperationModeStore;
import com.example.autorotate.Util.UsageStatsHelper;
import com.google.android.material.button.MaterialButton;

public class OnboardingActivity extends AppCompatActivity {

    private static final String PREF_ONBOARDING_DONE = "onboarding_done";

    private TextView usageStatus;
    private TextView writeStatus;
    private TextView batteryStatus;
    private MaterialButton continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        OperationModeStore.ensurePreferredMode(this);

        usageStatus = findViewById(R.id.onboarding_usage_status);
        writeStatus = findViewById(R.id.onboarding_write_status);
        batteryStatus = findViewById(R.id.onboarding_battery_status);
        continueButton = findViewById(R.id.onboarding_continue_button);

        MaterialButton usageButton = findViewById(R.id.onboarding_usage_button);
        MaterialButton writeButton = findViewById(R.id.onboarding_write_button);
        MaterialButton batteryButton = findViewById(R.id.onboarding_battery_button);

        usageButton.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        writeButton.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)));
        batteryButton.setOnClickListener(v -> {
            Intent batteryIntent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            batteryIntent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(batteryIntent);
        });
        continueButton.setOnClickListener(v -> {
            if (hasAllPermissions()) {
                setOnboardingDone();
                openMainScreen();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isOnboardingDone() && hasAllPermissions()) {
            openMainScreen();
            return;
        }

        boolean usageGranted = UsageStatsHelper.hasUsageStatsPermission(this);
        boolean writeGranted = !PermissionGate.needsWriteSettings(this) || Settings.System.canWrite(this);
        boolean batteryGranted = UsageStatsHelper.hasBatteryOptimizationExemption(this);

        usageStatus.setText(usageGranted ? R.string.permission_granted : R.string.permission_required);
        writeStatus.setText(writeGranted ? R.string.permission_granted : R.string.permission_required);
        batteryStatus.setText(batteryGranted ? R.string.permission_granted : R.string.permission_required);

        continueButton.setEnabled(usageGranted && writeGranted && batteryGranted);
    }

    private boolean hasAllPermissions() {
        return UsageStatsHelper.hasUsageStatsPermission(this)
                && (!PermissionGate.needsWriteSettings(this) || Settings.System.canWrite(this))
                && UsageStatsHelper.hasBatteryOptimizationExemption(this);
    }

    private boolean isOnboardingDone() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(PREF_ONBOARDING_DONE, false);
    }

    private void setOnboardingDone() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(PREF_ONBOARDING_DONE, true)
                .apply();
    }

    private void openMainScreen() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
