package com.example.autorotate.UI;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.autorotate.Model.AppsInfo;
import com.example.autorotate.R;
import com.example.autorotate.ViewModel.AppsRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class AppRotationConfigActivity extends AppCompatActivity {

    public static final String EXTRA_APP_NAME = "extra_app_name";
    public static final String EXTRA_APP_PACKAGE = "extra_app_package";
    public static final String EXTRA_IS_DEFAULT = "extra_is_default";

    private AppsInfo targetApp;
    private List<AppsInfo> savedApps = new ArrayList<>();

    private RadioGroup modeGroup;
    private RadioButton followDefaultButton;
    private RadioButton lockRotationButton;
    private RadioButton allowRotationButton;
    private View specificSection;
    private RadioGroup specificLockGroup;
    private LinearLayout specificAllowGroup;
    private RadioButton lock0;
    private RadioButton lock90;
    private RadioButton lock180;
    private RadioButton lock270;
    private CheckBox allow0;
    private CheckBox allow90;
    private CheckBox allow180;
    private CheckBox allow270;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rotation_config);

        MaterialToolbar toolbar = findViewById(R.id.rotation_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        modeGroup = findViewById(R.id.rotation_mode_group);
        followDefaultButton = findViewById(R.id.rotation_follow_default);
        lockRotationButton = findViewById(R.id.rotation_lock_rotation);
        allowRotationButton = findViewById(R.id.rotation_allow_rotation);
        specificSection = findViewById(R.id.rotation_specific_section);
        specificLockGroup = findViewById(R.id.rotation_specific_lock_group);
        specificAllowGroup = findViewById(R.id.rotation_specific_allow_group);
        lock0 = findViewById(R.id.lock_rotation_0);
        lock90 = findViewById(R.id.lock_rotation_90);
        lock180 = findViewById(R.id.lock_rotation_180);
        lock270 = findViewById(R.id.lock_rotation_270);
        allow0 = findViewById(R.id.allow_rotation_0);
        allow90 = findViewById(R.id.allow_rotation_90);
        allow180 = findViewById(R.id.allow_rotation_180);
        allow270 = findViewById(R.id.allow_rotation_270);
        MaterialButton saveButton = findViewById(R.id.rotation_save_button);

        loadTargetApp();
        toolbar.setTitle(targetApp != null && !targetApp.getAppName().isEmpty() ? targetApp.getAppName() : getString(R.string.rotation_config_screen_title));
        bindUi();
        updateSpecificVisibility();

        modeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            updateSpecificVisibility();
            bindSpecificSelection();
        });

        saveButton.setOnClickListener(v -> {
            applyUiState();
            AppsRepository.getInstance().setSavedApps(this, savedApps, null);
            finish();
        });
    }

    private void loadTargetApp() {
        boolean isDefaultEntry = getIntent().getBooleanExtra(EXTRA_IS_DEFAULT, false);
        String appName = getIntent().getStringExtra(EXTRA_APP_NAME);
        String appPackage = getIntent().getStringExtra(EXTRA_APP_PACKAGE);

        List<AppsInfo> currentApps = AppsRepository.getInstance().getSavedApps(this).getValue();
        if (currentApps != null) {
            savedApps = new ArrayList<>(currentApps);
        }

        if (isDefaultEntry) {
            targetApp = findDefaultApp();
            if (targetApp == null) {
                targetApp = AppsInfo.createDefaultEntry();
                savedApps.add(0, targetApp);
            }
            return;
        }

        targetApp = findApp(appPackage);
        if (targetApp == null) {
            targetApp = new AppsInfo(appName != null ? appName : "", appPackage != null ? appPackage : "");
            savedApps.add(targetApp);
        }
    }

    private AppsInfo findApp(String packageName) {
        if (packageName == null) {
            return null;
        }

        for (AppsInfo appsInfo : savedApps) {
            if (appsInfo != null && packageName.equals(appsInfo.getAppPackageName())) {
                return appsInfo;
            }
        }
        return null;
    }

    private AppsInfo findDefaultApp() {
        for (AppsInfo appsInfo : savedApps) {
            if (appsInfo != null && appsInfo.isDefaultEntry()) {
                return appsInfo;
            }
        }
        return null;
    }

    private void bindUi() {
        if (targetApp.isDefaultEntry()) {
            followDefaultButton.setVisibility(View.GONE);
        } else {
            followDefaultButton.setVisibility(View.VISIBLE);
        }

        if (targetApp.getRotationMode() == AppsInfo.MODE_ALLOW_ROTATION) {
            allowRotationButton.setChecked(true);
        } else if (targetApp.getRotationMode() == AppsInfo.MODE_LOCK_ROTATION) {
            lockRotationButton.setChecked(true);
        } else if (!targetApp.isDefaultEntry()) {
            followDefaultButton.setChecked(true);
        } else {
            lockRotationButton.setChecked(true);
        }

        bindSpecificSelection();
    }

    private void bindSpecificSelection() {
        int checkedMode = modeGroup.getCheckedRadioButtonId();
        List<Integer> rotations = targetApp.getSpecificRotations();

        if (checkedMode == R.id.rotation_allow_rotation) {
            allow0.setChecked(rotations.contains(0));
            allow90.setChecked(rotations.contains(90));
            allow180.setChecked(rotations.contains(180));
            allow270.setChecked(rotations.contains(270));
            if (!allow0.isChecked() && !allow90.isChecked() && !allow180.isChecked() && !allow270.isChecked()) {
                allow0.setChecked(true);
            }
            return;
        }

        int selectedRotation = rotations.isEmpty() ? 0 : rotations.get(0);
        lock0.setChecked(selectedRotation == 0);
        lock90.setChecked(selectedRotation == 90);
        lock180.setChecked(selectedRotation == 180);
        lock270.setChecked(selectedRotation == 270);
        if (!lock0.isChecked() && !lock90.isChecked() && !lock180.isChecked() && !lock270.isChecked()) {
            lock0.setChecked(true);
        }
    }

    private void updateSpecificVisibility() {
        int checkedMode = modeGroup.getCheckedRadioButtonId();
        boolean followDefault = checkedMode == R.id.rotation_follow_default && !targetApp.isDefaultEntry();

        if (followDefault) {
            specificSection.setVisibility(View.GONE);
            return;
        }

        specificSection.setVisibility(View.VISIBLE);
        if (checkedMode == R.id.rotation_allow_rotation) {
            specificLockGroup.setVisibility(View.GONE);
            specificAllowGroup.setVisibility(View.VISIBLE);
        } else {
            specificLockGroup.setVisibility(View.VISIBLE);
            specificAllowGroup.setVisibility(View.GONE);
        }
    }

    private void applyUiState() {
        int modeId = modeGroup.getCheckedRadioButtonId();
        if (modeId == R.id.rotation_allow_rotation) {
            targetApp.setRotationMode(AppsInfo.MODE_ALLOW_ROTATION);
            targetApp.setSpecificRotations(readAllowRotations());
        } else if (modeId == R.id.rotation_lock_rotation) {
            targetApp.setRotationMode(AppsInfo.MODE_LOCK_ROTATION);
            targetApp.setSpecificRotations(readLockRotation());
        } else if (!targetApp.isDefaultEntry()) {
            targetApp.setRotationMode(AppsInfo.MODE_FOLLOW_DEFAULT);
        } else {
            targetApp.setRotationMode(AppsInfo.MODE_LOCK_ROTATION);
            targetApp.setSpecificRotations(readLockRotation());
        }

        if (targetApp.isDefaultEntry() && targetApp.getRotationMode() == AppsInfo.MODE_FOLLOW_DEFAULT) {
            targetApp.setRotationMode(AppsInfo.MODE_LOCK_ROTATION);
        }
    }

    private List<Integer> readAllowRotations() {
        List<Integer> rotations = new ArrayList<>();
        if (allow0.isChecked()) {
            rotations.add(0);
        }
        if (allow90.isChecked()) {
            rotations.add(90);
        }
        if (allow180.isChecked()) {
            rotations.add(180);
        }
        if (allow270.isChecked()) {
            rotations.add(270);
        }
        if (rotations.isEmpty()) {
            rotations.add(0);
        }
        return rotations;
    }

    private List<Integer> readLockRotation() {
        List<Integer> rotations = new ArrayList<>();
        if (lock0.isChecked()) {
            rotations.add(0);
        } else if (lock90.isChecked()) {
            rotations.add(90);
        } else if (lock180.isChecked()) {
            rotations.add(180);
        } else if (lock270.isChecked()) {
            rotations.add(270);
        } else {
            rotations.add(0);
        }
        return rotations;
    }
}