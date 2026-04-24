package com.first.teja2.realautorotate.UI;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.first.teja2.realautorotate.R;
import com.first.teja2.realautorotate.Util.UsageStatsHelper;
import com.google.android.material.button.MaterialButton;

public class OnboardingActivity extends AppCompatActivity {

    private static final String PREF_ONBOARDING_DONE = "onboarding_done";

    private TextView usageStatus;
    private TextView writeStatus;
    private MaterialButton continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        usageStatus = findViewById(R.id.onboarding_usage_status);
        writeStatus = findViewById(R.id.onboarding_write_status);
        continueButton = findViewById(R.id.onboarding_continue_button);

        MaterialButton usageButton = findViewById(R.id.onboarding_usage_button);
        MaterialButton writeButton = findViewById(R.id.onboarding_write_button);

        usageButton.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        writeButton.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)));
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
        boolean writeGranted = Settings.System.canWrite(this);

        usageStatus.setText(usageGranted ? R.string.permission_granted : R.string.permission_required);
        writeStatus.setText(writeGranted ? R.string.permission_granted : R.string.permission_required);

        continueButton.setEnabled(usageGranted && writeGranted);
    }

    private boolean hasAllPermissions() {
        return UsageStatsHelper.hasUsageStatsPermission(this) && Settings.System.canWrite(this);
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
